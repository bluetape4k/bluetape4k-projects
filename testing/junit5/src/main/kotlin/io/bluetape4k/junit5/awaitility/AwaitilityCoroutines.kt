package io.bluetape4k.junit5.awaitility

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.awaitility.Durations
import org.awaitility.core.ConditionFactory
import org.awaitility.core.ConditionTimeoutException
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal val DEFAULT_POLL_INTERVAL: Duration = Durations.ONE_HUNDRED_MILLISECONDS
internal val DEFAULT_TIMEOUT: Duration = Durations.TEN_SECONDS

@Deprecated("use untilSuspending", ReplaceWith("untilSuspending"))
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
suspend inline infix fun ConditionFactory.awaitSuspending(
    crossinline block: suspend () -> Unit,
) {
    untilSuspending { block(); true }
}

/**
 * suspend 조건식이 `true`를 반환할 때까지 코루틴 방식으로 폴링 대기합니다.
 *
 * ## 동작/계약
 * - 초기 지연과 poll interval을 반영해 반복 호출하며, 호출 스레드를 block 하지 않습니다.
 * - 개별 poll을 별도 timeout으로 취소하지 않고, 전체 await timeout 안에서 suspend block을 실행합니다.
 * - 예외 무시 설정이 있으면 해당 예외는 마지막 원인으로 저장하고 계속 재시도합니다.
 * - `atLeast`는 조건이 너무 일찍 만족되면 Awaitility와 동일하게 최소 시간 위반 timeout을 던집니다.
 * - `during`은 조건이 지정된 hold 기간 동안 계속 true인지 확인하며, 중간 false에서 hold를 다시 시작합니다.
 * - `failFast`는 각 poll 전에 평가하고 terminal failure를 즉시 전파합니다.
 * - 타임아웃 초과 시 [ConditionTimeoutException]을 던집니다.
 * - 수신 [ConditionFactory]는 변경하지 않고, private 설정 접근은 adapter에 한정하며 설정 손실 시 명시적으로 실패합니다.
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
) = withContext(Dispatchers.IO) {
    val settings = readAwaitilityConditionSettings()
    val timeout = settings.maxWaitTime
    val timeoutNanos = timeout.toNanosSafely()
    val initialPollDelay = settings.pollDelay
    val initialPollDelayNanos = initialPollDelay.toNanosSafely()

    var pollCount = 0
    var lastInterval: Duration = initialPollDelay
    var lastThrowable: Throwable? = null
    var firstSatisfiedAtNanos: Long? = null
    val startNanos = System.nanoTime()

    if (initialPollDelayNanos > 0) {
        val delayNanos = minOf(initialPollDelayNanos, timeoutNanos)
        delay(nanosToMillisCeil(delayNanos).milliseconds)
    }

    while (true) {
        val remainingNanos = timeoutNanos - (System.nanoTime() - startNanos)
        if (remainingNanos <= 0L) {
            throw conditionTimeoutException(timeout, lastThrowable)
        }

        evaluateFailFastCondition(settings.failFastCondition)

        val poll = evaluatePoll(block, remainingNanos, timeout, settings.exceptionIgnorer, lastThrowable)
        lastThrowable = poll.lastThrowable

        val evaluatedAtNanos = System.nanoTime()
        if (poll.satisfied) {
            if (firstSatisfiedAtNanos == null) {
                firstSatisfiedAtNanos = evaluatedAtNanos
            }

            if (isHoldSatisfied(firstSatisfiedAtNanos, evaluatedAtNanos, settings.holdPredicateTime)) {
                val elapsedNanos = evaluatedAtNanos - startNanos
                if (elapsedNanos < settings.minWaitTime.toNanosSafely()) {
                    throw minimumWaitTimeoutException(elapsedNanos, settings.minWaitTime)
                }
                return@withContext
            }
        } else {
            firstSatisfiedAtNanos = null
        }

        val nextInterval = settings.pollInterval.next(++pollCount, lastInterval)
        lastInterval = nextInterval

        val sleepNanos = minOf(nextInterval.toNanosSafely(), timeoutNanos - (System.nanoTime() - startNanos))
        if (sleepNanos > 0) {
            delay(nanosToMillisCeil(sleepNanos).milliseconds)
        }
    }
}
