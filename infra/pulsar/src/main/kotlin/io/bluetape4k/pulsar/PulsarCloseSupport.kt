package io.bluetape4k.pulsar

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.warn
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.cancellation.CancellationException

@PublishedApi
internal suspend fun closeAsyncNonCancellable(
    resourceName: String,
    closeAsync: () -> CompletableFuture<Void>,
) {
    withContext(NonCancellable) {
        try {
            closeAsync().awaitSuspending()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) { "$resourceName close 실패" }
        }
    }
}
