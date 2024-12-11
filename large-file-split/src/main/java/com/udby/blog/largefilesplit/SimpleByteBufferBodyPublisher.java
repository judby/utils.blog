package com.udby.blog.largefilesplit;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Flow;

/**
 * Helper class for PUT/POST ByteBuffer content via HttpClient
 * <p>
 * Inspired by OneShotPublisher from <a href="https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/util/concurrent/Flow.html">Flow</a>
 */
public class SimpleByteBufferBodyPublisher implements HttpRequest.BodyPublisher {
    private final ByteBuffer byteBuffer;

    private boolean subscribed; // true after first subscribe

    SimpleByteBufferBodyPublisher(ByteBuffer byteBuffer) {
        this.byteBuffer = Objects.requireNonNull(byteBuffer, "byteBuffer");
    }

    @Override
    public long contentLength() {
        return byteBuffer.capacity();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        if (subscribed)
            subscriber.onError(new IllegalStateException());
        else {
            subscribed = true;
            subscriber.onSubscribe(new SimpleByteBufferSubscription(subscriber));
        }
    }

    class SimpleByteBufferSubscription implements Flow.Subscription {
        private final Flow.Subscriber<? super ByteBuffer> subscriber;
        private boolean completed;

        SimpleByteBufferSubscription(Flow.Subscriber<? super ByteBuffer> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public synchronized void request(long n) {
            if (!completed) {
                completed = true;
                if (n <= 0) {
                    subscriber.onError(new IllegalArgumentException());
                } else {
                    subscriber.onNext(byteBuffer);
                    subscriber.onComplete();
                }
            }
        }

        @Override
        public synchronized void cancel() {
            completed = true;
        }
    }
}
