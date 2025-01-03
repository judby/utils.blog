package com.udby.blog.largefilesplit.s3;

import com.udby.blog.largefilesplit.LeaseQueue;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestHelper {
    public static final MessageDigestHelper MD5 = new MessageDigestHelper("MD5");

    private final LeaseQueue<MessageDigest> leaseQueue = new LeaseQueue<>(256, this::clonedPrototype, MessageDigest::reset);
    private final MessageDigest prototype;

    public MessageDigestHelper(String algorithm) {
        try {
            final var messageDigest = MessageDigest.getInstance(algorithm);
            final var cloned = cloned(messageDigest);
            this.prototype = messageDigest;
            this.leaseQueue.offer(cloned);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public LeaseQueue<MessageDigest>.Lease lease() {
        return leaseQueue.lease();
    }

    private MessageDigest clonedPrototype() {
        return cloned(prototype);
    }

    private static MessageDigest cloned(MessageDigest messageDigest) {
        try {
            return (MessageDigest) messageDigest.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
