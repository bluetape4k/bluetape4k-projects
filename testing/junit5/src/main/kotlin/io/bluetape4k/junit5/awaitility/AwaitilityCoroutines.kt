package io.bluetape4k.junit5.awaitility

import kotlinx.coroutines.delay
import org.awaitility.Durations
import org.awaitility.constraint.WaitConstraint
import org.awaitility.core.ConditionFactory
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.core.ExceptionIgnorer
import org.awaitility.pollinterval.FixedPollInterval
import org.awaitility.pollinterval.PollInterval
import java.time.Duration

private val DEFAULT_POLL_INTERVAL: Duration = Durations.ONE_HUNDRED_MILLISECONDS
private val DEFAULT_TIMEOUT: Duration = Durations.TEN_SECONDS

@Deprecated("use awaitSuspending", ReplaceWith("awaitSuspending"))
suspend infix fun ConditionFactory.suspendAwait(block: suspend () -> Unit) =
    awaitSuspending(block)


@Deprecated("use untilSuspending", ReplaceWith("untilSuspending"))
suspend infix fun ConditionFactory.suspendUntil(block: suspend () -> Boolean) =
    untilSuspending(block)


/**
 * suspend 블록이 예외 없이 한 번 실행될 때까지 코루틴 폴링으로 대기합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [untilSuspending]을 호출해 `block(); true` 조건으로 처리합니다.
 * - 호출 스레드를 block 하지 않고 `delay` 기반으로 대기합니다.
 * - 타임아웃과 폴링 간격은 [ConditionFactory] 설정 값을 따릅니다.
 *
 * ```kotlin
 * var ready = false
 * kotlinx.coroutines.launch { kotlinx.coroutines.delay(50); ready = true }
 * await().atMost(Duration.ofSeconds(1)) awaitSuspending { if (!ready) error("wait") }
 * // 1초 내 ready=true가 되면 종료
 * ```
 *
 * @param block 성공 시점까지 반복 실행할 suspend 블록
 */
suspend infix fun ConditionFactory.awaitSuspending(
    block: suspend () -> Unit,
) {
    untilSuspending { block(); true }
}

/**
 * suspend 조건식이 `true`를 반환할 때까지 코루틴 방식으로 폴링 대기합니다.
 *
 * ## 동작/계약
 * - 초기 지연과 poll interval을 반영해 반복 호출하며, 호출 스레드를 block 하지 않습니다.
 * - 예외 무시 설정이 있으면 해당 예외는 마지막 원인으로 저장하고 계속 재시도합니다.
 * - 타임아웃 초과 시 [ConditionTimeoutException]을 던집니다.
 * - 수신 [ConditionFactory]는 변경하지 않고, 내부 루프 상태만 지역 변수로 관리합니다.
 *
 * ```kotlin
 * var attempts = 0
 * await().atMost(Duration.ofSeconds(1)) untilSuspending { ++attempts >= 3 }
 * // attempts == 3
 * ```
 *
 * @param block 만족 여부를 판단하는 suspend 조건식
 */
suspend infix fun ConditionFactory.untilSuspending(
    block: suspend () -> Boolean,
) {
    val timeout = timeoutConstraintOrDefault().maxWaitTime
    val pollInterval = pollIntervalOrDefault()
    val initialPollDelay = pollDelayOrDefault(pollInterval)
    val exceptionIgnorer = exceptionIgnorerOrNull()

    val startNanos = System.nanoTime()
    val timeoutNanos = timeout.toNanosSafely()

    if (!initialPollDelay.isZero && !initialPollDelay.isNegative) {
        delay(initialPollDelay.toMillisCeil())
    }

    var pollCount = 1
    var lastInterval: Duration = initialPollDelay
    var lastThrowable: Throwable? = null

    while (true) {
        val satisfied = try {
            val result = block()
            lastThrowable = null
            result
        } catch (e: Throwable) {
            if (exceptionIgnorer?.shouldIgnoreException(e) == true) {
                lastThrowable = e
                false
            } else {
                throw e
            }
        }

        if (satisfied) return

        val elapsedNanos = System.nanoTime() - startNanos
        if (elapsedNanos >= timeoutNanos) {
            val message = "Condition was not fulfilled within $timeout."
            throw if (lastThrowable != null) {
                ConditionTimeoutException(message, lastThrowable)
            } else {
                ConditionTimeoutException(message)
            }
        }

        val nextInterval = pollInterval.next(pollCount++, lastInterval)
        lastInterval = nextInterval

        val remainingNanos = timeoutNanos - (System.nanoTime() - startNanos)
        val sleepNanos = minOf(nextInterval.toNanosSafely(), remainingNanos)

        if (sleepNanos > 0) {
            delay(sleepNanos / 1_000_000L)
        }
    }
}


private fun ConditionFactory.timeoutConstraintOrDefault(): WaitConstraint =
    readPrivateField("timeoutConstraint") ?: object: WaitConstraint {
        override fun getMaxWaitTime(): Duration = DEFAULT_TIMEOUT
        override fun getMinWaitTime(): Duration = Duration.ZERO
        override fun getHoldPredicateTime(): Duration = Duration.ZERO

        override fun withMinWaitTime(minWaitTime: Duration): WaitConstraint = this
        override fun withMaxWaitTime(maxWaitTime: Duration): WaitConstraint = this
        override fun withHoldPredicateTime(holdConditionTime: Duration): WaitConstraint = this
    }

private fun ConditionFactory.pollIntervalOrDefault(): PollInterval =
    readPrivateField<PollInterval>("pollInterval")
        ?: FixedPollInterval(Duration.ofMillis(DEFAULT_POLL_INTERVAL.toMillis()))

private fun ConditionFactory.pollDelayOrDefault(pollInterval: PollInterval): Duration {
    return readPrivateField<Duration>("pollDelay")
        ?: if (pollInterval is FixedPollInterval) Duration.ZERO else Duration.ZERO
}

private fun ConditionFactory.exceptionIgnorerOrNull(): ExceptionIgnorer? =
    readPrivateField("exceptionsIgnorer")

private fun Duration.toNanosSafely(): Long = runCatching { toNanos() }.getOrElse { Long.MAX_VALUE }

private fun Duration.toMillisCeil(): Long {
    val nanos = toNanosSafely()
    return if (nanos <= 0L) 0L else (nanos + 999_999L) / 1_000_000L
}

@Suppress("UNCHECKED_CAST")
private fun <T> ConditionFactory.readPrivateField(name: String): T? = runCatching {
    val field = ConditionFactory::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.get(this) as T
}.getOrNull()
