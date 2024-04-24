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

import eu.bankopladerne.online.server.filecache.FileCache;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Path;

/**
 * Creates PNGs (900x200) with number specified drawn centered
 */
@RestApiController(NumbersImageController.BASE_PATH)
public class NumbersImageController {
    public static final String BASE_PATH = "/api/images/numbers";

    private final FileCache fileCache;
    private final Responses responses;

    public NumbersImageController(FileCache fileCache, Responses responses) {
        this.fileCache = fileCache;
        this.responses = responses;
    }

    @GetMapping(path = "{number}", produces = MediaType.IMAGE_PNG_VALUE)
    public void getPng(@PathVariable("number") int number) {
        final var produced = fileCache.produceAndCache(
                "numbers-%d.png".formatted(number),
                (objectName, tempFile) -> producePng(number, tempFile)
        );

        responses.streamFile(MediaType.IMAGE_PNG_VALUE, produced);
    }

    private Path producePng(int number, Path tempFile) {
        final var imagePng = new CenteredTextImagePng(900, 200);

        imagePng.drawCentered(Integer.toString(number), 120);
        imagePng.writeTo(tempFile);

        return tempFile;
    }
}
