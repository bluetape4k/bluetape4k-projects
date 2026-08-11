@file:Suppress("MatchingDeclarationName")

package io.bluetape4k.junit5.awaitility

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.core.ExceptionIgnorer
import org.awaitility.core.FailFastCondition
import org.awaitility.core.TerminalFailureException
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal data class PollEvaluation(
    val satisfied: Boolean,
    val lastThrowable: Throwable?,
)

@Suppress("TooGenericExceptionCaught", "SwallowedException")
internal suspend fun CoroutineScope.evaluatePoll(
    block: suspend () -> Boolean,
    remainingNanos: Long,
    timeout: Duration,
    exceptionIgnorer: ExceptionIgnorer,
    previousThrowable: Throwable?,
): PollEvaluation = try {
    PollEvaluation(
        satisfied = awaitPoll(block, remainingNanos, timeout, previousThrowable),
        lastThrowable = null,
    )
} catch (e: CancellationException) {
    throw e  // 부모 취소는 반드시 전파 — Result.failure로 포장하지 않음
} catch (e: PollTimedOutException) {
    throw conditionTimeoutException(e.timeout, e.lastThrowable)
} catch (e: Throwable) {
    if (exceptionIgnorer.shouldIgnoreException(e)) {
        PollEvaluation(satisfied = false, lastThrowable = e)
    } else {
        throw e
    }
}

@Suppress("TooGenericExceptionCaught")
internal suspend fun CoroutineScope.awaitPoll(
    block: suspend () -> Boolean,
    remainingNanos: Long,
    timeout: Duration,
    lastThrowable: Throwable?,
): Boolean {
    val pollDeferred = async {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
    val pollResult = select<Any?> {
        pollDeferred.onAwait { result -> result }
        onTimeout(nanosToMillisCeil(remainingNanos).milliseconds) {
            pollDeferred.cancel()
            PollTimedOut
        }
    }

    if (pollResult === PollTimedOut) {
        throw PollTimedOutException(timeout, lastThrowable)
    }

    @Suppress("UNCHECKED_CAST")
    return (pollResult as Result<Boolean>).getOrThrow()
}

internal fun isHoldSatisfied(
    firstSatisfiedAtNanos: Long?,
    evaluatedAtNanos: Long,
    holdPredicateTime: Duration,
): Boolean {
    val satisfiedAtNanos = firstSatisfiedAtNanos ?: evaluatedAtNanos
    val holdNanos = holdPredicateTime.toNanosSafely()
    return holdNanos <= 0L || evaluatedAtNanos - satisfiedAtNanos >= holdNanos
}

internal fun Duration.toNanosSafely(): Long = runCatching { toNanos() }.getOrElse { Long.MAX_VALUE }

private const val NANOS_PER_MILLISECOND = 1_000_000L
private const val NANOSECOND_CEILING = NANOS_PER_MILLISECOND - 1L

internal fun nanosToMillisCeil(nanos: Long): Long =
    if (nanos <= 0L) 0L else (nanos + NANOSECOND_CEILING) / NANOS_PER_MILLISECOND

internal fun conditionTimeoutException(timeout: Duration, cause: Throwable?): ConditionTimeoutException {
    val message = "Condition was not fulfilled within $timeout."
    val rootCause = cause.unwrapConditionTimeout()
    return if (rootCause != null) ConditionTimeoutException(message, rootCause) else ConditionTimeoutException(message)
}

internal fun minimumWaitTimeoutException(elapsedNanos: Long, minimumWait: Duration): ConditionTimeoutException {
    val elapsed = Duration.ofNanos(elapsedNanos.coerceAtLeast(1L))
    return ConditionTimeoutException(
        "Condition was evaluated in $elapsed which is earlier than expected minimum timeout $minimumWait",
    )
}

@Suppress("TooGenericExceptionCaught")
internal fun evaluateFailFastCondition(failFastCondition: FailFastCondition?) {
    when (failFastCondition) {
        null -> return
        is FailFastCondition.CallableFailFastCondition -> {
            if (failFastCondition.failFastCondition.call() == true) {
                throw TerminalFailureException(failFastCondition.failFastFailureReason)
            }
        }

        is FailFastCondition.CallableFailFastCondition.FailFastAssertion -> {
            try {
                failFastCondition.failFastAssertion.run()
            } catch (e: Throwable) {
                throw TerminalFailureException(failFastCondition.failFastFailureReason ?: e.message, e)
            }
        }

        else -> unsupportedFailFastCondition(failFastCondition)
    }
}

private fun unsupportedFailFastCondition(failFastCondition: FailFastCondition): Nothing =
    throw IllegalStateException(
        "Unsupported Awaitility fail-fast condition: ${failFastCondition::class.java.name}",
    )

private object PollTimedOut

private class PollTimedOutException(
    val timeout: Duration,
    val lastThrowable: Throwable?,
) : RuntimeException()

private tailrec fun Throwable?.unwrapConditionTimeout(): Throwable? = when (this) {
    is ConditionTimeoutException -> cause.unwrapConditionTimeout()
    else                         -> this
}
