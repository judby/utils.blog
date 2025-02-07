package com.udby.blog.largefilesplit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.foreign.Arena;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    public static final long FOUR_G = 4L * ONE_G;

    public static final long SIZE_5M = 5 * ONE_M;
    public static final long SIZE_8M = 8 * ONE_M;
    public static final long SIZE_16M = 16 * ONE_M;
    public static final long SIZE_32M = 32 * ONE_M;
    public static final long SIZE_64M = 64 * ONE_M;
    public static final long SIZE_100M = 100 * ONE_M;

    private final long partSize;
    private final long smallPartMaxSize;
    private final Path file;
    private final AtomicReference<Exception> exceptionCaught = new AtomicReference<>();

    /**
     * Create LargeFileSplitter given parameters:
     *
     * @param file             Path to file to split
     * @param partSize         Approx part size
     * @param smallPartMaxSize Max size of small parts to be included in last part (can make last part larger than partSize).
     *                         Set to zero if last parts can be small
     */
    public LargeFileSplitter(Path file, long partSize, long smallPartMaxSize) {
        if (!Files.isReadable(Objects.requireNonNull(file, "file"))) {
            throw new IllegalArgumentException("File not readable: %s".formatted(file));
        }
        if (TWO_G <= (partSize + smallPartMaxSize)) {
            throw new IllegalArgumentException("(partSize + smallPartMaxSize) must be below 2G, %d %d %d".formatted(partSize, smallPartMaxSize, (partSize + smallPartMaxSize)));
        }
        this.partSize = partSize;
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
        return new LargeFileSplitter(file, partSize, ONE_M);
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

        // current part within all parts of this byte buffer...
        int parts = 0;
        try (final var channel = FileChannel.open(file, READ); final var arena = Arena.ofShared()) {
            final var memorySegment = channel.map(FileChannel.MapMode.READ_ONLY, 0L, size, arena);

            // running offset into off-heap memory segment
            long offset = 0L;
            while (offset < size) {
                parts++;

                final var length = length(size, offset, partSize);
                final var slice = memorySegment.asSlice(offset, length);
                final var partBuffer = slice.asByteBuffer();

                final var partNumber = parts;

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

                offset += length;
            }

            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.HOURS);
        } catch (Exception e) {
            exceptionCaught.compareAndSet(null, e);
            executorService.shutdownNow();
            throw new IllegalStateException("Processing slices (part %d) of %s (shutting down execution)".formatted(parts, file), e);
        }
        return parts;
    }

    /**
     * If processing is being terminated by an Exception returns the Exception
     *
     * @return null if all good or the IOException terminating the processing
     */
    public Exception exception() {
        return exceptionCaught.get();
    }

    private long length(long size, long offset, long blockSize) {
        final var length = Math.min(blockSize, size - offset);
        final var remaining = size - (offset + length);
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
