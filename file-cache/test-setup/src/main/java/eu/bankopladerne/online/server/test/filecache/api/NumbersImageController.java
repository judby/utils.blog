package eu.bankopladerne.online.server.test.filecache.api;

import eu.bankopladerne.online.server.filecache.FileCache;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.file.Path;

@RestApiController("/api/images/numbers")
public class NumbersImageController {
    private final FileCache fileCache;
    private final Responses responses;

    public NumbersImageController(FileCache fileCache, Responses responses) {
        this.fileCache = fileCache;
        this.responses = responses;
    }

    @GetMapping(path = "{number}", produces = MediaType.IMAGE_PNG_VALUE)
    public void getPng(@PathVariable("number") int number) {
        final var produced = fileCache.produceAndCache("numbers-%d.png".formatted(number), ((objectName, tempFile) -> producePng(number, tempFile)));

        responses.streamFile(MediaType.IMAGE_PNG_VALUE, produced);
    }

    private Path producePng(int number, Path tempFile) {
        final var imagePng = new CenteredTextImagePng(900, 200);

        imagePng.drawCentered(Integer.toString(number), 120);
        imagePng.writeTo(tempFile);

        return tempFile;
    }
}
