package io.bluetape4k.resilience4j.retry;

import io.github.resilience4j.retry.Retry;

final class RetryAsyncContextBridge {

    private RetryAsyncContextBridge() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static long onResult(Retry.AsyncContext context, Object result) {
        return context.onResult(result);
    }
}
