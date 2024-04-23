package eu.bankopladerne.online.server.filecache;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.google.common.jimfs.PathType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileCacheTest {
    private static final int FILE_SYSTEM_MAX_SIZE = 1000;
    private static final String JIMFS_WORKING_DIR = "/work";
    private static final Configuration JIMFS_CONFIGURATION = Configuration.builder(PathType.unix())
            .setAttributeViews("basic")
            .setBlockSize(1)
            .setMaxSize(FILE_SYSTEM_MAX_SIZE)
            .setRoots("/")
            .setWorkingDirectory(JIMFS_WORKING_DIR)
            .build();

    private FileSystem fileSystem;

    @BeforeEach
    void setUp() {
        this.fileSystem = Jimfs.newFileSystem("test", JIMFS_CONFIGURATION);
    }

    @AfterEach
    void tearDown() {
        if (this.fileSystem != null) {
            try {
                this.fileSystem.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Test
    void produceAndCache_sunshine_succeeds() {
        // Given
        final var tmp = tempDirectory();
        final var fileCache = new FileCache(tmp, 3, 10, 1);

        final var counter = counter();
        final var producer = countingProducer(counter);

        // When
        fileCache.produceAndCache("A", producer);
        fileCache.produceAndCache("B", producer);
        fileCache.produceAndCache("C", producer);
        fileCache.produceAndCache("A", producer);

        // Then
        assertThat(counter.get()).isEqualTo(3);
        assertThat(usableSpace(tmp)).isEqualTo(FILE_SYSTEM_MAX_SIZE - 3);
    }

    @Test
    void produceAndCache_producingFiles_onlyCachesConfiguredNumber() {
        // Given
        final var tmp = tempDirectory();
        final var fileCache = new FileCache(tmp, 3, 10, 1);

        final var counter = counter();
        final var producer = countingProducer(counter);

        // When
        fileCache.produceAndCache("A", producer);
        fileCache.produceAndCache("B", producer);
        fileCache.produceAndCache("C", producer);
        fileCache.produceAndCache("D", producer);
        fileCache.produceAndCache("E", producer);
        fileCache.produceAndCache("F", producer);

        // Then: 6 files produced, 3 files retained
        assertThat(counter.get()).isEqualTo(6);
        assertThat(countFilesInFileSystem(tmp)).isEqualTo(3);
    }

    @Test
    void produceAndCache_producingFiles_doesNotUseAllSpace() {
        // Given - use space up to 90%
        final var tmp = tempDirectory();
        final var fileCache = new FileCache(tmp, 100, 10, 1);

        final var producing100Bytes = countingProducer(counter(), 100);

        IntStream.range(0, 9).forEach(i -> {
            fileCache.produceAndCache(Integer.toString(i), producing100Bytes);
        });

        // When
        fileCache.produceAndCache("Ax:", producing100Bytes);

        // Then
        assertThat(countFilesInFileSystem(tmp)).isEqualTo(9);
        assertThat(listOfFilesIn(tmp)
                .filter(p -> p.toString().contains("Ax~"))
                .count()).isOne();
    }

    @Test
    void produceAndCache_fileIsGone_producesAgain() {
        // Given
        final var tmp = tempDirectory();
        final var fileCache = new FileCache(tmp, 1, 10, 1);

        final var counter = counter();
        final var producer = countingProducer(counter);

        fileCache.produceAndCache("A", producer);

        listOfFilesIn(tmp).forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (Exception ignored) {
            }
        });

        // When
        fileCache.produceAndCache("A", producer);

        // Then
        assertThat(counter.get()).isEqualTo(2);
        assertThat(usableSpace(tmp)).isEqualTo(FILE_SYSTEM_MAX_SIZE - 1);
    }

    @Test
    void produceAndCache_producingFails_tempFileIsRemoved() {
        // Given
        final var tmp = tempDirectory();
        final var fileCache = new FileCache(tmp, 1, 10, 1);

        // When - Then
        assertThatThrownBy(() -> fileCache.produceAndCache("A", (o, p) -> {
            throw new IllegalStateException("failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(countFilesInFileSystem(tmp)).isZero();
    }

    @Test
    void produceAndCache_producingOtherFile_tempFileIsRemoved() {
        // Given
        final var tmp = tempDirectory();
        final var fileCache = new FileCache(tmp, 1, 10, 1);

        // When -
        fileCache.produceAndCache("A", (o, p) -> {
            try {
                final var otherFile = tmp.resolve("other.file");

                Files.writeString(otherFile, "x");

                return otherFile;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        // Then
        assertThat(countFilesInFileSystem(tmp)).isOne();
    }

    @Test
    void close_filesProduced_doesCleanUp() {
        // Given
        final var tmp = tempDirectory();
        final var fileCache = new FileCache(tmp, 100, 10, 1);

        final var counter = counter();
        final var producer = countingProducer(counter, 100);

        IntStream.range(0, 9).forEach(i -> {
            fileCache.produceAndCache(Integer.toString(i), producer);
        });

        // When
        try {
            fileCache.close();
        } catch (IOException e) {
            throw new AssertionError("Failed... " + e);
        }

        // Then
        assertThat(Files.exists(tmp)).isFalse();
        assertThat(usableSpace(tmp)).isEqualTo(FILE_SYSTEM_MAX_SIZE);
    }

    @Test
    void ensureOnlyValidFSCharacters_sunshine_succeeds() {
        // Given
        final var objectName = "file/\\:*?\"<>|.txt";

        // When
        final var ensureOnlyValidFSCharacters = FileCache.ensureOnlyValidFSCharacters(objectName);

        // Then
        assertThat(ensureOnlyValidFSCharacters).isEqualTo("file~~~~~~~~~.txt");
    }

    private Path tempDirectory() {
        return fileSystem.getPath(JIMFS_WORKING_DIR);
    }

    private static Stream<Path> listOfFilesIn(Path tmp) {
        try {
            return Files.list(tmp);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static AtomicInteger counter() {
        return new AtomicInteger();
    }

    private static long usableSpace(Path path) {
        try {
            return Files.getFileStore(path).getUsableSpace();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long countFilesInFileSystem(Path path) {
        try (final var list = Files.list(path)) {
            return list.count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static FileProducer countingProducer(AtomicInteger counter) {
        return countingProducer(counter, 1);
    }

    private static FileProducer countingProducer(AtomicInteger counter, int size) {
        return (o, p) -> {
            try {
                Files.write(p, new byte[size]);
                counter.incrementAndGet();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return p;
        };
    }
}
