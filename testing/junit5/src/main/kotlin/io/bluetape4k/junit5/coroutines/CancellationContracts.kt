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
 * coroutine cancellation contract 검증에 사용하는 기본 timeout입니다.
 */
val DEFAULT_CANCELLATION_CONTRACT_TIMEOUT: Duration = 5.seconds

/**
 * [block]을 실행하고 cancellation이 아닌 실패를 [Result.failure]로 포착합니다.
 *
 * ## 동작 계약
 * - [block]이 정상 완료되면 [Result.success]를 반환합니다.
 * - cancellation이 아닌 예외는 [Result.failure]로 반환합니다.
 * - structured coroutine cancellation이 보존되도록 [CancellationException]은 다시 던집니다.
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
 * suspend [block]을 실행하고 cancellation이 아닌 실패를 [Result.failure]로 포착합니다.
 *
 * ## 동작 계약
 * - [block]이 정상 완료되면 [Result.success]를 반환합니다.
 * - cancellation이 아닌 예외는 [Result.failure]로 반환합니다.
 * - [CancellationException]은 다시 던집니다. structured cancellation을 보존해야 하는 suspend API 주변에서는
 *   일반 `runCatching` 대신 이 helper를 사용합니다.
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
 * 시작한 coroutine을 취소했을 때 [operation] 내부에서 [CancellationException]으로 관측되는지 검증합니다.
 *
 * ## 동작 계약
 * - [operation]을 child coroutine에서 즉시 시작합니다.
 * - child가 첫 suspension point에 도달한 뒤 취소합니다.
 * - [operation]이 정상 완료되거나 cancellation을 다른 result/exception type으로 바꾸면 실패합니다.
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
 * suspended waiter 하나를 취소하면 등록이 해제되고 다음 waiter를 막지 않는지 검증합니다.
 *
 * ## 동작 계약
 * - [awaiter]를 한 번 시작하고 suspended 상태에서 취소합니다.
 * - [awaiter]를 다시 시작한 뒤 [releaser]를 호출하고, 두 번째 waiter가 [timeout] 안에 완료되는지 확인합니다.
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
 * coroutine 취소가 [operation]이 사용하는 underlying resource도 취소하는지 검증합니다.
 *
 * ## 동작 계약
 * - [operation]을 child coroutine에서 즉시 시작합니다.
 * - test가 future, HTTP call, callback registration 시작을 확인할 수 있도록 [beforeCancel]을 실행합니다.
 * - coroutine을 취소하고 [resourceCancelled]가 참인지 확인합니다.
 * - cancellation이 non-cancellation outcome으로 변환되면 실패합니다.
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
