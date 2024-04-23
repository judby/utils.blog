package eu.bankopladerne.online.server.test.filecache.api;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;

public class CenteredTextImagePng {
    private static final Color COLOR = Color.BLACK;
    private static final Font POPPINS_FONT;
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    static {
        Font font;
        try {
            try (InputStream inputStream = CenteredTextImagePng.class.getResourceAsStream("/fonts/poppins/Poppins-Regular.ttf")) {
                font = Font.createFont(Font.TRUETYPE_FONT, inputStream);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load font");
        }
        POPPINS_FONT = font;
    }

    private final BufferedImage img;

    public CenteredTextImagePng(int width, int height) {
        this.img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public void writeTo(Path path) {
        try (final var fileImageOutputStream = new FileImageOutputStream(path.toFile())) {
            final var writer = ImageIO.getImageWritersByFormatName("png")
                    .next();

            writer.setOutput(fileImageOutputStream);
            writer.write(img);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Graphics2D prepareGraphics() {
        final var graphics = img.createGraphics();

        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        return graphics;
    }

    public void drawCentered(String s, int size) {
        final var height = img.getHeight();
        final var width = img.getWidth();

        final var graphics = prepareGraphics();
        try {
            graphics.setColor(TRANSPARENT);
            graphics.fillRect(0, 0, width, height);

            graphics.setColor(COLOR);

            final var font = POPPINS_FONT.deriveFont(Font.BOLD, (float) size);
            graphics.setFont(font);

            final var metrics = graphics.getFontMetrics();

            final var x = (width - metrics.stringWidth(s)) / 2;
            final var y = ((height - metrics.getHeight()) / 2) + metrics.getAscent();

            graphics.drawString(s, x, y);
        } finally {
            graphics.dispose();
        }
    }
}
