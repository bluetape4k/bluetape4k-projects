package io.bluetape4k.junit5.http.idempotency

import io.bluetape4k.assertions.shouldBeEqualTo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal data class ConformanceScenario(
    val name: String,
    val run: suspend (
        BoundedWaitHttpIdempotencyAdapter,
        BoundedWaitHttpIdempotencyConformanceConfig,
    ) -> Unit,
)

internal suspend fun runConformanceScenarios(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
    scenarios: List<ConformanceScenario>,
) {
    validateNormalFixtureCompatibility(config)
    require(config.maxWaitersPerKey <= MAX_CONFORMANCE_WAITERS_PER_KEY) {
        "The conformance runner supports at most $MAX_CONFORMANCE_WAITERS_PER_KEY waiters per key."
    }

    val watchdog = newWatchdog()
    var runnerFailure: Throwable? = null
    var finalCleanupFailure: Throwable? = null
    var shutdownFailure: Throwable? = null
    try {
        try {
            scenarios.forEach { scenario -> runOneScenario(watchdog, adapter, config, scenario) }
        } catch (failure: Throwable) {
            runnerFailure = failure
        }

        finalCleanupFailure = captureFailure {
            withContext(NonCancellable) {
                withMonotonicWatchdog(watchdog, config.scenarioTimeout, FINAL_CLEANUP_SCENARIO) {
                    adapter.resetScenario()
                }
            }
        }
    } finally {
        shutdownFailure = shutdownWatchdog(watchdog)
    }

    val primary = runnerFailure ?: finalCleanupFailure ?: shutdownFailure
    primary?.let { failure ->
        listOfNotNull(finalCleanupFailure, shutdownFailure)
            .filterNot { it === failure }
            .forEach(failure::addSuppressed)
        throw failure
    }
}

private suspend fun runOneScenario(
    watchdog: ScheduledExecutorService,
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
    scenario: ConformanceScenario,
) {
    val scenarioFailure = captureFailure {
        withMonotonicWatchdog(watchdog, config.scenarioTimeout, scenario.name) {
            scenario.run(adapter, config)
        }
    }
    val cleanupFailure = captureFailure {
        withContext(NonCancellable) {
            withMonotonicWatchdog(watchdog, config.scenarioTimeout, "${scenario.name}-cleanup") {
                adapter.resetScenario()
                adapter.quiescence() shouldBeEqualTo QUIESCENT
            }
        }
    }

    scenarioFailure?.let { failure ->
        cleanupFailure?.let(failure::addSuppressed)
        throw failure
    }
    cleanupFailure?.let { throw it }
}

private suspend fun <T> withMonotonicWatchdog(
    watchdog: ScheduledExecutorService,
    timeout: Duration,
    scenario: String,
    block: suspend () -> T,
): T = coroutineScope {
    val work = async(start = CoroutineStart.UNDISPATCHED) { block() }
    val expired = CompletableDeferred<Unit>()
    val timeoutTask = watchdog.schedule(
        { expired.complete(Unit) },
        timeout.toNanos(),
        TimeUnit.NANOSECONDS,
    )
    try {
        select {
            work.onAwait { it }
            expired.onAwait {
                work.cancel(CancellationException("HTTP idempotency scenario watchdog expired"))
                throw AssertionError(
                    "HTTP idempotency conformance timed out; scenario=$scenario; values redacted",
                )
            }
        }
    } finally {
        timeoutTask.cancel(false)
        if (work.isActive) work.cancelAndJoin()
    }
}

internal suspend fun exchangeChecked(
    adapter: BoundedWaitHttpIdempotencyAdapter,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
    request: HttpIdempotencyRequest,
    allowConfiguredRequestOverflow: Boolean = false,
): HttpIdempotencyResponse {
    if (!allowConfiguredRequestOverflow) validateRequestAgainstInstance(request, config)
    return adapter.exchange(request).also { response -> validateReplaySnapshot(response, config) }
}

private fun validateNormalFixtureCompatibility(config: BoundedWaitHttpIdempotencyConformanceConfig) {
    representativeNormalRequests().forEach { request -> validateRequestAgainstInstance(request, config) }
    representativeTerminalResponses().forEach { response -> validateReplaySnapshot(response, config) }
}

private fun validateRequestAgainstInstance(
    request: HttpIdempotencyRequest,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    request.idempotencyKeys.forEach { key ->
        requireBoundedUtf8(key, config.maxIdempotencyKeyBytes, "idempotencyKey")
    }
    requireBoundedUtf8(request.requestBody, config.maxRequestBodyBytes, "requestBody")
}

private fun validateReplaySnapshot(
    response: HttpIdempotencyResponse,
    config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    requireBoundedUtf8(response.body, config.maxReplayBodyBytes, "responseBody")
    val replayHeaders = response.headers.filterKeys { name ->
        name == CONTENT_TYPE_HEADER || name in config.replayHeaderAllowlist
    }
    require(replayHeaders.size <= config.maxReplayHeaderNames) {
        "Replay snapshot has too many header names."
    }
    var aggregateBytes = 0L
    replayHeaders.forEach { (name, values) ->
        require(values.size <= config.maxReplayValuesPerHeader) {
            "Replay snapshot has too many values."
        }
        aggregateBytes += name.toByteArray(Charsets.UTF_8).size
        values.forEach { value ->
            aggregateBytes += requireBoundedUtf8(
                value,
                config.maxReplayHeaderValueBytes,
                "replayHeaderValue",
            )
        }
    }
    require(aggregateBytes <= config.maxReplayHeaderBytes) {
        "Replay snapshot exceeds aggregate bytes."
    }
}

private fun newWatchdog(): ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread.ofPlatform()
            .daemon()
            .name(WATCHDOG_THREAD_NAME)
            .unstarted(runnable)
    }

private fun shutdownWatchdog(watchdog: ScheduledExecutorService): Throwable? =
    try {
        watchdog.shutdownNow()
        check(watchdog.awaitTermination(WATCHDOG_SHUTDOWN_SECONDS, TimeUnit.SECONDS)) {
            "HTTP idempotency watchdog did not terminate."
        }
        null
    } catch (failure: Throwable) {
        failure
    }

private suspend inline fun captureFailure(crossinline block: suspend () -> Unit): Throwable? =
    try {
        block()
        null
    } catch (cancelled: CancellationException) {
        cancelled
    } catch (failure: Throwable) {
        failure
    }

private val QUIESCENT = HttpIdempotencyQuiescence(0, 0, 0)
private const val MAX_CONFORMANCE_WAITERS_PER_KEY = 32
private const val WATCHDOG_THREAD_NAME = "http-idempotency-watchdog"
private const val WATCHDOG_SHUTDOWN_SECONDS = 5L
private const val CONTENT_TYPE_HEADER = "content-type"
private const val FINAL_CLEANUP_SCENARIO = "final-cleanup"
