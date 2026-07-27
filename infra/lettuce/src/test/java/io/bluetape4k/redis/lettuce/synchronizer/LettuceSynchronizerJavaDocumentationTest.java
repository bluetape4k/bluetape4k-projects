package io.bluetape4k.redis.lettuce.synchronizer;

import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LettuceSynchronizerJavaDocumentationTest {

    @Test
    void publicBlockingAndAsyncMethodsRemainJavaCallable() throws Exception {
        assertEquals(
            CompletableFuture.class,
            LettuceDistributedSemaphore.class
                .getMethod(
                    "acquireAsync",
                    SemaphoreOwnerId.class,
                    SemaphoreRequestId.class,
                    int.class,
                    Duration.class
                )
                .getReturnType()
        );
        LettucePermitExpirableSemaphore.class.getMethod(
            "create",
            StatefulRedisConnection.class,
            String.class,
            ExpirableSemaphoreConfig.class
        );
        LettuceCountDownLatch.class.getMethod(
            "await",
            LatchGeneration.class,
            LatchRequestId.class,
            Duration.class
        );
        assertEquals(
            CompletableFuture.class,
            LettuceCountDownLatch.class
                .getMethod("getCountAsync", LatchGeneration.class)
                .getReturnType()
        );
    }
}
