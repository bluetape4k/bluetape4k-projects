package io.bluetape4k.pulsar

import io.bluetape4k.assertions.shouldBeEqualTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import java.util.concurrent.CompletableFuture

internal suspend fun TestScope.assertCleanupWaitsAfterCancellation(
    closeFuture: CompletableFuture<Void>,
    block: suspend (CompletableDeferred<Unit>) -> Unit,
) {
    val entered = CompletableDeferred<Unit>()
    val job = launch {
        block(entered)
    }

    entered.await()
    job.cancel()
    runCurrent()

    try {
        job.isCompleted shouldBeEqualTo false
    } finally {
        closeFuture.complete(null)
    }

    job.join()
    job.isCompleted shouldBeEqualTo true
}
