package com.udby.blog.largefilesplit.s3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class MessageDigestHelper {
    public static final MessageDigestHelper MD5 = new MessageDigestHelper("MD5");

    private final Queue<MessageDigest> cachedMessageDigests = new ArrayBlockingQueue<>(256);
    private final MessageDigest prototype;

    public MessageDigestHelper(String algorithm) {
        try {
            final var messageDigest = MessageDigest.getInstance(algorithm);
            final var cloned = cloned(messageDigest);
            this.prototype = messageDigest;
            this.cachedMessageDigests.offer(cloned);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public MessageDigestHolder lease() {
        return new MessageDigestHolder();
    }

    private static MessageDigest cloned(MessageDigest messageDigest) {
        try {
            return (MessageDigest) messageDigest.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    private MessageDigest clonedPrototypeIfNull(MessageDigest messageDigest) {
        return messageDigest != null ? messageDigest : cloned(prototype);
    }

    public class MessageDigestHolder implements AutoCloseable {
        private final MessageDigest messageDigest;
        private volatile boolean closed;

        private MessageDigestHolder() {
            this.messageDigest = clonedPrototypeIfNull(cachedMessageDigests.poll());
        }

        public MessageDigest messageDigest() {
            if (closed) {
                throw new IllegalStateException("Is closed");
            }
            return messageDigest;
        }

        @Override
        public void close() {
            this.closed = true;
            this.messageDigest.reset();
            cachedMessageDigests.offer(this.messageDigest);
        }
    }
}
