package eu.bankopladerne.online.server.filecache;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Component managing a cache in the server temp directory of files produced by the application
 */
public class FileCache implements AutoCloseable {
    /**
     * FileStore related to the temporary directory
     */
    private final FileStore fileStore;
    /**
     * Cache of concurrent locks on object names
     */
    private final SimplestCache<String, Lock> lockCache;
    /**
     * Cache of Paths of objects/files
     */
    private final SimplestCache<String, Path> tempCache;
    /**
     * Path to the temporary cache of objects/files
     */
    private final Path tempCacheDir;

    /**
     * Create FileCache
     *
     * @param maxFilesToCache     Max number of files/objects to cache
     * @param minFreeSpacePercent Minimum percentage of free space in the temporary file system before deleting files/objects
     * @param maxConcurrency      Max number of concurrent threads requesting objects/files to be produced
     */
    public FileCache(int maxFilesToCache, double minFreeSpacePercent, int maxConcurrency) {
        this(createTempDirectory(), maxFilesToCache, minFreeSpacePercent, maxConcurrency);
    }

    /**
     * Create FileCache
     *
     * @param tempPath            Location of temporary files
     * @param maxFilesToCache     Max number of files/objects to cache
     * @param minFreeSpacePercent Minimum percentage of free space in the temporary file system before deleting files/objects
     * @param maxConcurrency      Max number of concurrent threads requesting objects/files to be produced
     */
    public FileCache(final Path tempPath, final int maxFilesToCache, final double minFreeSpacePercent, final int maxConcurrency) {
        this.lockCache = new SimplestCache<>(maxConcurrency);
        this.tempCacheDir = tempPath;
        this.tempCache = new SimplestCache<>(maxFilesToCache, e -> freeSpacePercent() < minFreeSpacePercent, FileCache::deleteFileEntry);
        try {
            this.fileStore = Files.getFileStore(tempPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Return cached object or produce and return a new object. The objectName must uniquely identify the file/object
     * being created.
     * The producer can choose to produce the file as suggested or another, must return a Path to the actual content
     * produced.
     * Locking is in place to help ensure the same item is not being produced twice in high-traffic situations, but
     * it cannot be ensured. The maxConcurrency setting must be aligned with the max number of concurrent requests
     * expected.
     *
     * @param objectName       Name of item to produce - must uniquely identify the object to cache/produce
     * @param producerFunction Reference(lambda) to the producing implementation
     * @return Path to the cached, produced file/object
     */
    public Path produceAndCache(String objectName, FileProducer producerFunction) {
        final var lock = lockOn(objectName);
        lock.lock();
        try {
            final var file = cached(objectName);
            if (file == null || notReadable(file)) {
                // Create a temporary file for the object to produce
                final var temp = Files.createTempFile(tempCacheDir, ensureOnlyValidFSCharacters(objectName), "");
                final Path producedFile;
                try {
                    producedFile = producerFunction.produceToCache(objectName, temp);
                } catch (RuntimeException e) {
                    deleteFileSilently(temp);
                    throw e;
                }

                // producer might choose to dump into a different file...
                if (!producedFile.equals(temp)) {
                    // remove temp if it is not the one being cached
                    deleteFileSilently(temp);
                }

                // cache the item produced
                cacheIt(objectName, producedFile);

                return producedFile;
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * While closing this FileCache, do remove all files/objects cached
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        try (final var list = Files.list(tempCacheDir)) {
            list.forEach(FileCache::deleteFileSilently);
        }

        Files.deleteIfExists(tempCacheDir);
    }

    /**
     * Calculate the amount of free space in the temporary directory, in percentage (0..100).
     *
     * @return percentage of free space in the temporary directory (0..100)
     */
    private double freeSpacePercent() {
        try {
            final long totalSpace = fileStore.getTotalSpace();
            final long usableSpace = fileStore.getUsableSpace();
            return 100.0 * usableSpace / totalSpace;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Remove characters from object name that any filesystem might not approve on
     *
     * @param name Name of object
     * @return Name of object where weird characters are replaced with '~'
     */
    static String ensureOnlyValidFSCharacters(String name) {
        return Objects.requireNonNull(name).replaceAll("[\\\\/:*?\"<>|]", "~");
    }

    private void cacheIt(String objectName, Path file) {
        tempCache.put(objectName, file);
    }

    private Path cached(String objectName) {
        return tempCache.get(objectName);
    }

    private Lock lockOn(String objectName) {
        return lockCache.computeIfAbsent(objectName, k -> new ReentrantLock());
    }

    private static Path createTempDirectory() {
        try {
            return Files.createTempDirectory("filecache");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Called when the cache overflows (too many items, or we are running out of temp space)
     *
     * @param e
     */
    private static void deleteFileEntry(Map.Entry<String, Path> e) {
        deleteFileSilently(e.getValue());
    }

    private static void deleteFileSilently(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // We do not expect problems while deleting ancient temporary files
        }
    }

    private static boolean notReadable(Path file) {
        final var readable = Files.isReadable(file);
        if (!readable) {
            // This is slightly surprising since FileCache has a reference to a file previously produced but where
            // some other infrastructure has decided to delete it. This is gracefully handled by the implementation
            return true;
        }
        return false;
    }
}
