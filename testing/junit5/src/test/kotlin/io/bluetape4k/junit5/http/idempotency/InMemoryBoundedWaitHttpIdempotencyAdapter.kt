package io.bluetape4k.junit5.http.idempotency

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal class InMemoryBoundedWaitHttpIdempotencyAdapter(
    private val config: BoundedWaitHttpIdempotencyConformanceConfig,
): BoundedWaitHttpIdempotencyAdapter {

    private val mutex = Mutex()
    private val records = mutableMapOf<String, TestRecord>()
    private val ownerSignals = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val sideEffects = ConcurrentHashMap<String, AtomicInteger>()
    private val activeWaiters = AtomicInteger()
    private val openGates = AtomicInteger()

    var completedScenarioCount: Int = 0
        private set

    override suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse {
        authenticateAndAuthorize(request)?.let { return it }
        val scope = serverResolvedScope(request)
        val fingerprint = fingerprint(request)
        return when (val action = mutex.withLock { decideExchange(scope, fingerprint) }) {
            is ExchangeAction.Owner -> action.record.completion.await()
            is ExchangeAction.Waiter -> awaitOwner(action.record)
            is ExchangeAction.Immediate -> action.response
        }
    }

    override suspend fun awaitOwnerStarted(request: HttpIdempotencyRequest) {
        val scope = serverResolvedScope(request)
        val signal = mutex.withLock { ownerSignals.getOrPut(scope) { CompletableDeferred() } }
        signal.await()
    }

    override suspend fun awaitWaiterCount(request: HttpIdempotencyRequest, expected: Int) {
        val record = mutex.withLock { records[serverResolvedScope(request)] }
            ?: error("Owner was not started; values redacted.")
        record.waiterCount.first { count -> count == expected }
    }

    override suspend fun completeOwner(
        request: HttpIdempotencyRequest,
        outcome: HttpIdempotencyResponse,
    ) {
        val completion = mutex.withLock {
            val record = records[serverResolvedScope(request)]
                ?: error("Owner was not started; values redacted.")
            check(record.state == TestState.InFlight) { "Owner is not in flight; values redacted." }
            record.state = TestState.Terminal
            record.response = outcome
            openGates.decrementAndGet()
            record.completion
        }
        completion.complete(outcome.withReplayFlag(false))
    }

    override suspend fun abandonOwner(
        request: HttpIdempotencyRequest,
        outcome: HttpIdempotencyResponse,
    ) {
        val completion = mutex.withLock {
            val scope = serverResolvedScope(request)
            val record = records.remove(scope)
                ?: error("Owner was not started; values redacted.")
            record.state = TestState.Abandoned
            openGates.decrementAndGet()
            record.completion
        }
        completion.complete(outcome)
    }

    override suspend fun advanceTimeBy(duration: Duration) {
        require(!duration.isNegative) { "duration must not be negative." }
        yield()
    }

    override suspend fun resetScenario() {
        yield()
        val pending = mutex.withLock {
            if (records.isNotEmpty() || ownerSignals.isNotEmpty() || sideEffects.isNotEmpty()) {
                completedScenarioCount++
            }
            records.values.map { it.completion }.also {
                records.clear()
                ownerSignals.clear()
                sideEffects.clear()
                activeWaiters.set(0)
                openGates.set(0)
            }
        }
        pending.forEach { completion ->
            completion.cancel(CancellationException("HTTP idempotency scenario reset"))
        }
    }

    override fun sideEffectCount(request: HttpIdempotencyRequest): Int =
        sideEffects[serverResolvedScopeOrDenied(request)]?.get() ?: 0

    override fun quiescence(): HttpIdempotencyQuiescence = HttpIdempotencyQuiescence(
        activeWaiters = activeWaiters.get(),
        openGates = openGates.get(),
        activeChildTasks = 0,
    )

    private fun decideExchange(scope: String, fingerprint: String): ExchangeAction {
        val existing = records[scope]
        if (existing == null) {
            val signal = ownerSignals.getOrPut(scope) { CompletableDeferred() }
            val record = TestRecord(fingerprint = fingerprint)
            records[scope] = record
            sideEffects.computeIfAbsent(scope) { AtomicInteger() }.incrementAndGet()
            openGates.incrementAndGet()
            signal.complete(Unit)
            return ExchangeAction.Owner(record)
        }
        if (existing.fingerprint != fingerprint) {
            return ExchangeAction.Immediate(idempotencyConflictResponse())
        }
        return when (existing.state) {
            TestState.InFlight -> {
                existing.waiterCount.value++
                activeWaiters.incrementAndGet()
                ExchangeAction.Waiter(existing)
            }
            TestState.Terminal -> ExchangeAction.Immediate(
                checkNotNull(existing.response).withReplayFlag(true),
            )
            TestState.Abandoned -> error("Abandoned records must not remain visible.")
        }
    }

    private suspend fun awaitOwner(record: TestRecord): HttpIdempotencyResponse =
        try {
            record.completion.await().withReplayFlag(true)
        } finally {
            record.waiterCount.value--
            activeWaiters.decrementAndGet()
        }

    private fun authenticateAndAuthorize(request: HttpIdempotencyRequest): HttpIdempotencyResponse? =
        when (request.authenticationProfile) {
            "tenant-a-principal", "tenant-b-principal" -> null
            "unauthenticated" -> unauthenticatedResponse()
            "tenant-a-read-only" -> unauthorizedResponse()
            else -> unauthorizedResponse()
        }

    private fun serverResolvedScope(request: HttpIdempotencyRequest): String {
        val tenant = when (request.authenticationProfile) {
            "tenant-a-principal" -> "tenant-a"
            "tenant-b-principal" -> "tenant-b"
            else -> error("Authentication must complete before idempotency lookup; values redacted.")
        }
        val key = request.idempotencyKeys.singleOrNull()
            ?: error("A control request must contain one key; values redacted.")
        return listOf(
            tenant,
            digest(request.operation),
            digest(request.resourceIdentity),
            digest(key),
        ).joinToString("|")
    }

    private fun serverResolvedScopeOrDenied(request: HttpIdempotencyRequest): String =
        if (request.authenticationProfile == "tenant-a-principal" ||
            request.authenticationProfile == "tenant-b-principal"
        ) {
            serverResolvedScope(request)
        } else {
            DENIED_SCOPE
        }

    private fun fingerprint(request: HttpIdempotencyRequest): String = digest(request.requestBody)

    private fun digest(value: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)),
    )

    private class TestRecord(
        val fingerprint: String,
        var state: TestState = TestState.InFlight,
        var response: HttpIdempotencyResponse? = null,
        val completion: CompletableDeferred<HttpIdempotencyResponse> = CompletableDeferred(),
        val waiterCount: MutableStateFlow<Int> = MutableStateFlow(0),
    )

    private sealed interface TestState {
        data object InFlight: TestState
        data object Terminal: TestState
        data object Abandoned: TestState
    }

    private sealed interface ExchangeAction {
        data class Owner(val record: TestRecord): ExchangeAction
        data class Waiter(val record: TestRecord): ExchangeAction
        data class Immediate(val response: HttpIdempotencyResponse): ExchangeAction
    }

    private companion object {
        const val DENIED_SCOPE = "denied"
    }
}
