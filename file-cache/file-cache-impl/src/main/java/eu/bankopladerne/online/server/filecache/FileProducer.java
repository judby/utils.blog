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
package eu.bankopladerne.online.server.filecache;

import java.nio.file.Path;

@FunctionalInterface
public interface FileProducer {
    /**
     * Produces a file given the object name. The tempFile given is created in the temporary file-system. Return the
     * path to the file actually produced.
     *
     * @param objectName Name of object/file being created
     * @param tempFile   Tempoary file to use
     * @return The actual file produced, same as tempFile if that is used
     */
    Path produceToCache(String objectName, Path tempFile);
}
