package eu.bankopladerne.online.server.test.filecache.api;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class Responses {
    private final HttpServletResponse httpServletResponse;

    public Responses(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }

    public void streamFile(final String contentType, final Path fileToStream) {
        httpServletResponse.setContentType(contentType);

        try (final var out = httpServletResponse.getOutputStream()) {
            httpServletResponse.setContentLengthLong(Files.size(fileToStream));
            Files.copy(fileToStream, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
