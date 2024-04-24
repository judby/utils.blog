/*
 * Copyright 2024 Jesper Udby
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.bankopladerne.online.server.test.filecache.configuration;

import eu.bankopladerne.online.server.filecache.FileCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuring the FileCache with some sane defaults (configurable)
 */
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
