package com.udby.blog.largefilesplit.s3;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class MessageDigestHelperTest {

    @Test
    void digest_sunshine_succeeds() {
        // Given
        final var byteBuffer = ByteBuffer.wrap("The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8));

        // When
        final var digest = MessageDigestHelper.MD5.digest(byteBuffer);

        // Then
        assertThat(digest).asHexString()
                .isEqualTo("9E107D9D372BB6826BD81D3542A419D6");
    }
}
