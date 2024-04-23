package eu.bankopladerne.online.server.test.filecache.configuration;

import eu.bankopladerne.online.server.filecache.FileCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileCacheConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileCacheConfiguration.class);

    @Bean
    public FileCache fileCache(FileCacheConfig cacheConfig) {
        LOGGER.info(cacheConfig.toString());
        return new FileCache(cacheConfig.maxFilesToCache(), cacheConfig.minSpacePercent(), cacheConfig.maxConcurrency());
    }

    @ConfigurationProperties("online.server.file-cache.config")
    public record FileCacheConfig(
            @DefaultValue("1000") int maxFilesToCache,
            @DefaultValue("20.0") double minSpacePercent,
            @DefaultValue("10") int maxConcurrency) {
    }
}
