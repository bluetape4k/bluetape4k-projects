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
 * [block]이 true 를 반환할 때까지 대기한다
 *
 * ```
 * await atMost 5.seconds suspendAwait { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
suspend infix fun ConditionFactory.awaitSuspending(
    block: suspend () -> Unit,
) {
    untilSuspending { block(); true }
}

/**
 * [block]이 true 를 반환할 때까지 코루틴 방식으로 polling 하며 대기한다.
 *
 * Awaitility의 동기 `until` 대신 suspend loop 를 사용하므로 호출 스레드를 block 하지 않는다.
 * timeout/poll 설정은 [ConditionFactory]에 지정된 값을 반영한다.
 *
 * ```
 * await atMost 5.seconds suspendUntil { ... }
 * ```
 *
 * @param block 판단을 위한 코드 블럭
 */
suspend infix fun ConditionFactory.untilSuspending(
    block: suspend () -> Boolean,
) {
    val timeout = timeoutConstraintOrDefault().maxWaitTime
    val pollInterval = pollIntervalOrDefault()
    val initialPollDelay = pollDelayOrDefault(pollInterval)
    val exceptionIgnorer = exceptionIgnorerOrNull()

    val startNanos = System.nanoTime()
    if (!initialPollDelay.isZero && !initialPollDelay.isNegative) {
        delay(initialPollDelay.toMillisCeil())
    }

    var pollCount = 1
    var previousInterval = initialPollDelay

    while (true) {
        val satisfied = try {
            block()
        } catch (e: Throwable) {
            if (exceptionIgnorer?.shouldIgnoreException(e) == true) {
                false
            } else {
                throw e
            }
        }

        if (satisfied) {
            return
        }

        val elapsedNanos = System.nanoTime() - startNanos
        val timeoutNanos = timeout.toNanosSafely()
        if (elapsedNanos >= timeoutNanos) {
            throw ConditionTimeoutException("Condition was not fulfilled within $timeout.")
        }

        val interval = pollInterval.next(pollCount, previousInterval)
        val remainingNanos = timeoutNanos - elapsedNanos
        val sleepNanos = minOf(interval.toNanos(), remainingNanos)

        if (sleepNanos > 0) {
            delay(Duration.ofNanos(sleepNanos).toMillisCeil())
        }

        previousInterval = interval
        pollCount++
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
    readPrivateField<PollInterval>("pollInterval") ?: FixedPollInterval(DEFAULT_POLL_INTERVAL)

private fun ConditionFactory.pollDelayOrDefault(pollInterval: PollInterval): Duration {
    val configuredPollDelay = readPrivateField<Duration>("pollDelay")
    return when {
        configuredPollDelay != null       -> configuredPollDelay
        pollInterval is FixedPollInterval -> pollInterval.next(1, Duration.ZERO)
        else                              -> Duration.ZERO
    }
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
