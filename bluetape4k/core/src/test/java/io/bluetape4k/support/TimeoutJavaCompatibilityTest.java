package io.bluetape4k.support;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeoutJavaCompatibilityTest {

    @Test
    void legacyTimeoutOverloadRemainsCallableFromJava() throws Exception {
        CompletableFuture<String> future = TimeoutSupportKt.asyncRunWithTimeout(100L, () -> "ok");

        assertEquals("ok", future.get());
    }
}
