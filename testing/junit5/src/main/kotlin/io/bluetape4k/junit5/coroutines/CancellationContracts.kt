package io.bluetape4k.junit5.coroutines

import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Default timeout for coroutine cancellation contract checks.
 */
val DEFAULT_CANCELLATION_CONTRACT_TIMEOUT: Duration = 5.seconds

/**
 * Runs [block] and captures non-cancellation failures as [Result.failure].
 *
 * ## Behaviour / Contract
 * - Returns [Result.success] when [block] completes normally.
 * - Returns [Result.failure] for non-cancellation exceptions.
 * - Rethrows [CancellationException] so structured coroutine cancellation is
 *   preserved.
 *
 * ```kotlin
 * val result = resultOfNonCancellation { parseValue() }
 * ```
 */
inline fun <T> resultOfNonCancellation(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

/**
 * Runs suspend [block] and captures non-cancellation failures as [Result.failure].
 *
 * ## Behaviour / Contract
 * - Returns [Result.success] when [block] completes normally.
 * - Returns [Result.failure] for non-cancellation exceptions.
 * - Rethrows [CancellationException]. Use this instead of plain `runCatching`
 *   around suspend APIs that must preserve structured cancellation.
 *
 * ```kotlin
 * val result = runCatchingNonCancellation { client.fetch() }
 * ```
 */
suspend inline fun <T> runCatchingNonCancellation(
    crossinline block: suspend () -> T,
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

/**
 * Asserts that cancelling the launched coroutine is observed as
 * [CancellationException] inside [operation].
 *
 * ## Behaviour / Contract
 * - Starts [operation] immediately in a child coroutine.
 * - Cancels the child after it reaches the first suspension point.
 * - Fails if [operation] completes normally or turns cancellation into another
 *   result/exception type.
 *
 * ```kotlin
 * assertCancellationPropagates {
 *     wrapperReturningResult {
 *         delay(Long.MAX_VALUE)
 *     }
 * }
 * ```
 */
suspend fun <T> assertCancellationPropagates(
    timeout: Duration = DEFAULT_CANCELLATION_CONTRACT_TIMEOUT,
    operation: suspend CoroutineScope.() -> T,
) {
    var outcome: Result<T>? = null

    coroutineScope {
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                outcome = Result.success(operation())
            } catch (e: CancellationException) {
                outcome = Result.failure(e)
            } catch (e: Throwable) {
                outcome = Result.failure(e)
            }
        }

        yield()
        job.cancel(CancellationException("Coroutine cancellation contract probe"))
        withTimeout(timeout) {
            job.join()
        }
    }

    outcome?.exceptionOrNull().shouldBeInstanceOf<CancellationException>()
}

/**
 * Asserts that cancelling one suspended waiter clears its registration and does
 * not block the next waiter.
 *
 * ## Behaviour / Contract
 * - Starts [awaiter] once and cancels it while suspended.
 * - Starts [awaiter] again, calls [releaser], and verifies that the second
 *   waiter completes within [timeout].
 *
 * ```kotlin
 * assertCancellationClearsWaiter(
 *     awaiter = { gate.await() },
 *     releaser = { gate.resume() },
 * )
 * ```
 */
suspend fun assertCancellationClearsWaiter(
    timeout: Duration = DEFAULT_CANCELLATION_CONTRACT_TIMEOUT,
    awaiter: suspend () -> Unit,
    releaser: () -> Unit,
) {
    coroutineScope {
        val first = launch(start = CoroutineStart.UNDISPATCHED) {
            awaiter()
        }

        first.cancel(CancellationException("Waiter cancellation contract probe"))
        withTimeout(timeout) {
            first.join()
        }

        val second = launch(start = CoroutineStart.UNDISPATCHED) {
            awaiter()
        }

        releaser()
        withTimeout(timeout) {
            second.join()
        }
        second.isCompleted.shouldBeTrue()
    }
}

/**
 * Asserts that cancelling a coroutine also cancels the underlying resource used
 * by [operation].
 *
 * ## Behaviour / Contract
 * - Starts [operation] immediately in a child coroutine.
 * - Runs [beforeCancel] so tests can wait until a future, HTTP call, or callback
 *   registration is known to have started.
 * - Cancels the coroutine and verifies [resourceCancelled].
 * - Fails if cancellation is converted to a non-cancellation outcome.
 *
 * ```kotlin
 * assertResourceCancelledOnCoroutineCancellation(
 *     beforeCancel = { waitUntilRequestStarted() },
 *     resourceCancelled = { call.isCanceled },
 * ) {
 *     call.suspendExecute()
 * }
 * ```
 */
suspend fun <T> assertResourceCancelledOnCoroutineCancellation(
    timeout: Duration = DEFAULT_CANCELLATION_CONTRACT_TIMEOUT,
    beforeCancel: suspend () -> Unit = { yield() },
    resourceCancelled: () -> Boolean,
    operation: suspend CoroutineScope.() -> T,
) {
    var outcome: Result<T>? = null

    coroutineScope {
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                outcome = Result.success(operation())
            } catch (e: CancellationException) {
                outcome = Result.failure(e)
            } catch (e: Throwable) {
                outcome = Result.failure(e)
            }
        }

        withTimeout(timeout) {
            beforeCancel()
        }
        job.cancel(CancellationException("Resource cancellation contract probe"))
        withTimeout(timeout) {
            job.join()
        }
    }

    resourceCancelled().shouldBeTrue()
    outcome?.exceptionOrNull().shouldBeInstanceOf<CancellationException>()
}
