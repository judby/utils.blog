package eu.bankopladerne.online.server.filecache;

import java.nio.file.Path;

@FunctionalInterface
public interface FileProducer {
    Path produceToCache(String objectName, Path tempFile);
}
