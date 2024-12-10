package com.udby.blog.largefilesplit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static com.udby.blog.largefilesplit.LargeFileSplitter.ONE_M;
import static com.udby.blog.largefilesplit.LargeFileSplitter.SIZE_16M;
import static com.udby.blog.largefilesplit.LargeFileSplitter.SIZE_32M;
import static com.udby.blog.largefilesplit.LargeFileSplitter.SIZE_8M;
import static com.udby.blog.largefilesplit.LargeFileSplitter.TWO_G;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.assertj.core.api.Assertions.assertThat;

class LargeFileSplitterTest {
    @TempDir
    private Path output;

    @Test
    void processInVirtualThreads_smallLastPart_includedInLast() {
        // Given
        final var smallPart = 256;
        final var fileSize = TWO_G - SIZE_32M + smallPart;
        final var largeFile = createLargeTempFile(fileSize);

        final var partSizes = new ArrayList<Integer>();

        // When
        final var largeFileSplitter = LargeFileSplitter.fromFile(largeFile, SIZE_32M);
        final var parts = largeFileSplitter.processInVirtualThreads((_, buffer) -> {
            synchronized (partSizes) {
                partSizes.add(buffer.limit());
            }
        });

        // Then
        assertThat(parts).isEqualTo(64);
        assertThat(partSizes).hasSize(64);

        final var sum = partSizes.stream()
                .mapToInt(Integer::intValue)
                .sum();
        assertThat(sum).isEqualTo((int) fileSize);

        final var smallestPart = partSizes.stream()
                .mapToInt(Integer::intValue)
                .min();
        assertThat(smallestPart).isPresent();
        assertThat(smallestPart.getAsInt()).isGreaterThan(smallPart);
    }

    @ParameterizedTest
    @ValueSource(longs = {SIZE_8M, SIZE_16M, SIZE_32M})
    void processInVirtualThreads_splitToTemporaryFiles_succeeds(long splitSize) throws Exception {
        // Given
        // A large file max 25% of available in temporary file system
        final var fileStore = fileStore();

        final var usableSpace = fileStore.getUsableSpace();
        final var twentyFivePercent = usableSpace / 4;
        // Size of actual file inspiring this implementation
        final var actualFileSize = 9841183379L;
        final var size = Math.min(actualFileSize, twentyFivePercent);

        final var largeFile = createLargeTempFile(size);

        System.out.printf("Created file (size %.3fM) to be split by %.1fM%n", size * 1.0 / ONE_M, splitSize * 1.0 / ONE_M);

        final var t0 = System.nanoTime();

        final var largeFileSplitter = LargeFileSplitter.fromFile(largeFile, splitSize);

        // When
        final var parts = largeFileSplitter.processInVirtualThreads((partNumber, byteBuffer) -> {
            final var out = output.resolve("part-%04d.split".formatted(partNumber));
            try (final var channel = FileChannel.open(out, WRITE, CREATE_NEW)) {
                channel.write(byteBuffer);
            }
        });

        System.out.printf("Timing: %fs Part count: %d%n", 1e-9 * (System.nanoTime() - t0), parts);

        // Then
        final long partsSize;
        try (final var stream = Files.list(output)) {
            partsSize = stream
                    .filter(p -> p.getFileName().toString().startsWith("part-"))
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .sum();
        }

        // Then
        assertThat(partsSize).isEqualTo(size);
    }

    private Path createLargeTempFile(long size) {
        try {
            final var largeFile = Files.createTempFile(output, "large-%d-".formatted(size), ".file");
            try (final var channel = FileChannel.open(largeFile, WRITE)) {
                channel.position(size - 1);
                channel.write(ByteBuffer.allocate(1));
            }
            return largeFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private FileStore fileStore() {
        try {
            return Files.getFileStore(output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
