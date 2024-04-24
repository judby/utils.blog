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
package eu.bankopladerne.online.server.test.filecache.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper component for interacting with the HttpServletResponse
 */
@Component
public class Responses {
    private final HttpServletResponse httpServletResponse;

    public Responses(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }

    public void streamFile(final String contentType, final Path fileToStream) {
        httpServletResponse.setContentType(contentType);

        // This is also the place to set up response headers that help downstream clients to cache the content being
        // served. These are specifically left out since we do not want the clients to cache anything in this setup...

        try (final var out = httpServletResponse.getOutputStream()) {
            httpServletResponse.setContentLengthLong(Files.size(fileToStream));
            Files.copy(fileToStream, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
