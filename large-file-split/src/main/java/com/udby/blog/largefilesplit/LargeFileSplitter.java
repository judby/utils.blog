package com.udby.blog.largefilesplit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.file.StandardOpenOption.READ;

/**
 * Simple tool for splitting very large files (>100M) and processing as smaller parts.
 * Specifically created for using the amazon <a href="https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpu-upload-object.html">S3 multipart file upload</a>.
 * <p/>
 * Simple usage:
 * <pre>
 * {@code
 *     // create splitter that splits into parts of ~16MiB
 *     var largeFileSplitter = LargeFileSplitter.fromFile(pathToLargeFile, 16_777_216L);
 *     // do the splitting and wait for its termination
 *     var partCount = largeFileSplitter.processInVirtualThreads((partNumber, byteBuffer) -> {
 *         // process part in byteBuffer
 *     });
 *     // check exception status...
 *     if (largeFileSplitter.exception() != null) {
 *         // handle Exception
 *     }
 * }
 * </pre>
 */
public class LargeFileSplitter {
    public static final long ONE_K = 1024L;
    public static final long ONE_M = ONE_K * ONE_K;
    public static final long ONE_G = ONE_M * ONE_K;
    public static final long TWO_G = 2L * ONE_G;

    public static final long SIZE_8M = 8 * ONE_M;
    public static final long SIZE_16M = 16 * ONE_M;
    public static final long SIZE_32M = 32 * ONE_M;

    private final long partSize;
    private final long mappedSize;
    private final long smallPartMaxSize;
    private final Path file;
    private final AtomicReference<Exception> exceptionCaught = new AtomicReference<>();

    /**
     * Create LargeFileSplitter given parameters:
     *
     * @param file             Path to file to split
     * @param partSize         Approx part size
     * @param mappedSize       Size of each memory mapped part
     * @param smallPartMaxSize Max size of small parts to be included in last part (can make last part larger than partSize).
     *                         Set to zero if last parts can be small
     */
    public LargeFileSplitter(Path file, long partSize, int mappedSize, long smallPartMaxSize) {
        if (!Files.isReadable(Objects.requireNonNull(file, "file"))) {
            throw new IllegalArgumentException("File not readable: %s".formatted(file));
        }
        this.partSize = partSize;
        this.mappedSize = mappedSize;
        this.file = file;
        this.smallPartMaxSize = smallPartMaxSize;
    }

    /**
     * Create LargeFileSplitter from large file with sane defaults (max small part size is 1MiB)
     *
     * @param file     Path to file to split
     * @param partSize Approx part size
     * @return Instance with sane defaults
     */
    public static LargeFileSplitter fromFile(Path file, long partSize) {
        final var mappedSize = integer(((TWO_G / partSize) - 1) * partSize, "2G - partSize");
        return new LargeFileSplitter(file, partSize, mappedSize, ONE_M);
    }

    /**
     * Split the file using virtual threads using the given part processor and await termination
     *
     * @param processor FilePartProcessor handling each part of the file
     * @return number of parts created
     */
    public int processInVirtualThreads(FilePartProcessor processor) {
        try (final var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            return process(executorService, processor);
        }
    }

    /**
     * Split the file using executor service of choice...
     *
     * @param executorService Executor service providing executors for processing file parts
     * @param processor       FilePartProcessor handling each part of the file
     * @return number of parts created
     */
    public int process(ExecutorService executorService, FilePartProcessor processor) {
        final var size = fileSize();

        int partStart = 0;
        try (final var channel = FileChannel.open(file, READ)) {
            long index = 0;
            while (index < size && exceptionCaught.get() == null) {
                final var length = length(size, index, mappedSize);

                final var mappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, index, length);

                partStart += processFileParts(partStart, executorService, processor, mappedByteBuffer);
                index += length;
            }
        } catch (Exception e) {
            exceptionCaught.compareAndSet(null, e);
            executorService.shutdownNow();
            throw new IllegalStateException("Processing slices (part %d) of %s (shutting down execution)".formatted(partStart, file), e);
        }
        return partStart;
    }

    /**
     * If processing is being terminated by an Exception returns the Exception
     *
     * @return null if all good or the IOException terminating the processing
     */
    public Exception exception() {
        return exceptionCaught.get();
    }

    private int processFileParts(
            final int partStart,
            final ExecutorService executorService,
            final FilePartProcessor processor,
            final MappedByteBuffer byteBuffer) {
        final long size = byteBuffer.capacity();
        final var approxBlocks = (int) ((size + partSize - 1) / partSize);
        // Align on 16 bytes boundary
        final var blockSize = (((size + 15) / approxBlocks) & 0xfffffffffffffff0L);
        // index into current byteBuffer
        long index = 0;
        // current part within all parts of this byte buffer...
        int parts = 0;

        while (index < size && exceptionCaught.get() == null) {
            parts++;

            final var length = length(size, index, blockSize);

            final var partBuffer = byteBuffer.slice(integer(index, "index"), integer(length, "length"));
            final var partNumber = parts + partStart;

            // Send this part for processing via the executor service
            executorService.execute(() -> {
                try {
                    processor.processPart(partNumber, partBuffer);
                } catch (Exception e) {
                    exceptionCaught.compareAndSet(null, e);
                    executorService.shutdownNow();
                    throw new IllegalStateException("Processing part %d of %s (shutting down execution)".formatted(partNumber, file), e);
                }
            });

            index += length;
        }

        return parts;
    }

    private long length(long size, long index, long blockSize) {
        final var length = Math.min(blockSize, size - index);
        final var remaining = size - (index + length);
        // don't want very small last parts
        if (remaining > 0 && remaining <= smallPartMaxSize) {
            return length + remaining;
        }
        return length;
    }

    private long fileSize() {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to get size of file %s".formatted(file), e);
        }
    }

    private static int integer(long value, String name) {
        if (value < 0 || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Not valid positive integer: %d (%s)".formatted(value, name));
        }
        return (int) value;
    }

    @FunctionalInterface
    public interface FilePartProcessor {
        /**
         * Helper for wrapping a ByteBuffer as InputStream
         *
         * @param byteBuffer buffer to wrap
         * @return buffer wrapped as InputStream
         */
        static InputStream asInputStream(final ByteBuffer byteBuffer) {
            return new InputStream() {
                public int read() {
                    if (!byteBuffer.hasRemaining()) {
                        return -1;
                    }
                    return byteBuffer.get() & 0xff;
                }

                public int read(byte[] bytes, int offset, int length) {
                    if (!byteBuffer.hasRemaining()) {
                        return -1;
                    }

                    final var len = Math.min(length, byteBuffer.remaining());
                    byteBuffer.get(bytes, offset, len);
                    return len;
                }
            };
        }

        void processPart(int partNumber, ByteBuffer byteBuffer) throws IOException;
    }
}
