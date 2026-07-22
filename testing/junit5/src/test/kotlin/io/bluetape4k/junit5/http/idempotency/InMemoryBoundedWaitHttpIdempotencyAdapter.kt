package io.bluetape4k.junit5.http.idempotency

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal class InMemoryBoundedWaitHttpIdempotencyAdapter(
    private val config: BoundedWaitHttpIdempotencyConformanceConfig,
): BoundedWaitHttpIdempotencyAdapter {

    private val mutex = Mutex()
    private val records = mutableMapOf<String, TestRecord>()
    private val ownerSignals = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val responseDeliveryGates = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val sideEffects = ConcurrentHashMap<String, AtomicInteger>()
    private val activeWaiters = AtomicInteger()
    private val maximumWaiters = AtomicInteger()
    private val openGates = AtomicInteger()
    private val waiterSequence = AtomicLong()
    private val nextOwnerCleanupAttempt = AtomicReference<CompletableDeferred<Unit>?>()
    private val nextWaiterCleanupAttempt = AtomicReference<CompletableDeferred<Unit>?>()
    private var virtualNow: Duration = Duration.ZERO

    var completedScenarioCount: Int = 0
        private set

    val maximumObservedWaiters: Int
        get() = maximumWaiters.get()

    override suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse {
        authenticateAndAuthorize(request)?.let { return it }
        val scope = serverResolvedScope(request)
        val fingerprint = fingerprint(request)
        return when (val action = mutex.withLock { decideExchange(scope, fingerprint) }) {
            is ExchangeAction.Owner -> awaitOwnerCompletion(action)
            is ExchangeAction.Waiter -> awaitWaiterCompletion(action)
            is ExchangeAction.Immediate -> action.response
        }
    }

    override suspend fun awaitOwnerStarted(request: HttpIdempotencyRequest) {
        val scope = serverResolvedScope(request)
        val signal = mutex.withLock { ownerSignals.getOrPut(scope) { CompletableDeferred() } }
        signal.await()
    }

    override suspend fun awaitWaiterCount(request: HttpIdempotencyRequest, expected: Int) {
        require(expected >= 0) { "expected must not be negative." }
        val record = mutex.withLock { records[serverResolvedScope(request)] }
            ?: error("Owner was not started; values redacted.")
        val observed = record.waiterCount.first { count -> count == expected || count == RESET_WAITER_COUNT }
        check(observed != RESET_WAITER_COUNT) { "Scenario reset before waiter count was observed." }
    }

    override suspend fun completeOwner(
        request: HttpIdempotencyRequest,
        outcome: HttpIdempotencyResponse,
    ) {
        val selected = mutex.withLock {
            val record = records[serverResolvedScope(request)]
                ?: error("Owner was not started; values redacted.")
            check(record.state == TestState.InFlight) { "Owner is not in flight; values redacted." }
            record.state = TestState.Terminal
            record.response = outcome
            openGates.decrementAndGet()
            val waiterOutcomes = record.waiters.values.map { waiter ->
                waiter to if (virtualNow >= waiter.deadline) {
                    inFlightTimeoutResponse(config)
                } else {
                    outcome.withReplayFlag(true)
                }
            }
            removeAllWaiters(record)
            CompletionSelection(
                owner = record.ownerCompletion to outcome.withReplayFlag(false),
                waiters = waiterOutcomes,
            )
        }
        selected.owner.first.complete(selected.owner.second)
        selected.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
    }

    override suspend fun holdOwnerResponseDelivery(request: HttpIdempotencyRequest) {
        mutex.withLock {
            val scope = serverResolvedScope(request)
            check(scope !in records) { "Owner already started; values redacted." }
            check(scope !in responseDeliveryGates) { "Response delivery already held; values redacted." }
            responseDeliveryGates[scope] = CompletableDeferred()
            openGates.incrementAndGet()
        }
    }

    override suspend fun releaseOwnerResponseDelivery(request: HttpIdempotencyRequest) {
        val gate = mutex.withLock { removeResponseDeliveryGate(serverResolvedScope(request)) }
        gate?.complete(Unit)
    }

    override suspend fun abandonOwner(
        request: HttpIdempotencyRequest,
        outcome: HttpIdempotencyResponse,
    ) {
        val selected = mutex.withLock {
            abandonRecord(serverResolvedScope(request), outcome, completeOwner = true)
        }
        selected.owner?.let { (completion, response) -> completion.complete(response) }
        selected.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
    }

    override suspend fun advanceTimeBy(duration: Duration) {
        require(!duration.isNegative) { "duration must not be negative." }
        val expired = mutex.withLock {
            virtualNow = virtualNow.plus(duration)
            records.values.flatMap { record ->
                if (record.state != TestState.InFlight) return@flatMap emptyList()
                val timedOut = record.waiters.values.filter { waiter -> virtualNow >= waiter.deadline }
                timedOut.forEach { waiter -> removeWaiter(record, waiter.id) }
                timedOut
            }
        }
        expired.forEach { waiter -> waiter.completion.complete(inFlightTimeoutResponse(config)) }
        yield()
    }

    override suspend fun resetScenario() {
        yield()
        val pending = mutex.withLock {
            if (records.isNotEmpty() || ownerSignals.isNotEmpty() || sideEffects.isNotEmpty()) {
                completedScenarioCount++
            }
            PendingReset(
                owners = records.values.map { it.ownerCompletion },
                waiters = records.values.flatMap { record -> record.waiters.values.map { it.completion } },
                responseDeliveries = responseDeliveryGates.values.toList(),
                ownerSignals = ownerSignals.values.toList(),
            ).also {
                records.values.forEach { record -> record.waiterCount.value = RESET_WAITER_COUNT }
                records.clear()
                ownerSignals.clear()
                responseDeliveryGates.clear()
                sideEffects.clear()
                activeWaiters.set(0)
                openGates.set(0)
                virtualNow = Duration.ZERO
            }
        }
        val cancellation = CancellationException("HTTP idempotency scenario reset")
        pending.owners.forEach { completion -> completion.cancel(cancellation) }
        pending.waiters.forEach { completion -> completion.cancel(cancellation) }
        pending.responseDeliveries.forEach { completion -> completion.cancel(cancellation) }
        pending.ownerSignals.forEach { completion -> completion.cancel(cancellation) }
    }

    override fun sideEffectCount(request: HttpIdempotencyRequest): Int =
        sideEffects[serverResolvedScopeOrDenied(request)]?.get() ?: 0

    override fun quiescence(): HttpIdempotencyQuiescence = HttpIdempotencyQuiescence(
        activeWaiters = activeWaiters.get(),
        openGates = openGates.get(),
        activeChildTasks = 0,
    )

    internal suspend fun holdStateLockForTest(
        entered: CompletableDeferred<Unit>,
        release: CompletableDeferred<Unit>,
    ) {
        mutex.withLock {
            entered.complete(Unit)
            release.await()
        }
    }

    internal fun observeNextOwnerCleanupAttempt(): CompletableDeferred<Unit> =
        observeNextCleanupAttempt(nextOwnerCleanupAttempt)

    internal fun observeNextWaiterCleanupAttempt(): CompletableDeferred<Unit> =
        observeNextCleanupAttempt(nextWaiterCleanupAttempt)

    private fun decideExchange(scope: String, fingerprint: String): ExchangeAction {
        val existing = records[scope]
        if (existing == null) {
            val signal = ownerSignals.getOrPut(scope) { CompletableDeferred() }
            val record = TestRecord(
                fingerprint = fingerprint,
                responseDeliveryGate = responseDeliveryGates[scope],
            )
            records[scope] = record
            sideEffects.computeIfAbsent(scope) { AtomicInteger() }.incrementAndGet()
            openGates.incrementAndGet()
            signal.complete(Unit)
            return ExchangeAction.Owner(scope, record)
        }
        if (existing.fingerprint != fingerprint) {
            return ExchangeAction.Immediate(idempotencyConflictResponse())
        }
        return when (existing.state) {
            TestState.InFlight -> registerWaiterOrReject(scope, existing)
            TestState.Terminal -> ExchangeAction.Immediate(
                checkNotNull(existing.response).withReplayFlag(true),
            )
            TestState.Abandoned -> error("Abandoned records must not remain visible.")
        }
    }

    private fun registerWaiterOrReject(scope: String, record: TestRecord): ExchangeAction {
        if (record.waiters.size >= config.maxWaitersPerKey) {
            return ExchangeAction.Immediate(waiterOverflowResponse(config))
        }
        val waiter = TestWaiter(
            id = waiterSequence.incrementAndGet(),
            deadline = virtualNow.plus(config.waitTimeout),
        )
        record.waiters[waiter.id] = waiter
        updateWaiterCount(record)
        val observed = activeWaiters.incrementAndGet()
        maximumWaiters.updateAndGet { current -> maxOf(current, observed) }
        return ExchangeAction.Waiter(scope, record, waiter)
    }

    private suspend fun awaitOwnerCompletion(action: ExchangeAction.Owner): HttpIdempotencyResponse =
        try {
            val response = action.record.ownerCompletion.await()
            action.record.responseDeliveryGate?.await()
            response
        } catch (cancelled: CancellationException) {
            val selected = withContext(NonCancellable) {
                nextOwnerCleanupAttempt.getAndSet(null)?.complete(Unit)
                mutex.withLock {
                    val current = records[action.scope]
                    if (current === action.record && current.state == TestState.InFlight) {
                        abandonRecord(action.scope, transientFailureResponse(), completeOwner = false)
                    } else {
                        removeResponseDeliveryGate(action.scope)
                        AbandonSelection.EMPTY
                    }
                }
            }
            selected.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
            throw cancelled
        }

    private suspend fun awaitWaiterCompletion(action: ExchangeAction.Waiter): HttpIdempotencyResponse =
        try {
            action.waiter.completion.await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            withContext(NonCancellable) {
                nextWaiterCleanupAttempt.getAndSet(null)?.complete(Unit)
                mutex.withLock {
                    val current = records[action.scope]
                    if (current === action.record) removeWaiter(current, action.waiter.id)
                }
            }
        }

    private fun abandonRecord(
        scope: String,
        outcome: HttpIdempotencyResponse,
        completeOwner: Boolean,
    ): AbandonSelection {
        val record = records[scope] ?: return AbandonSelection.EMPTY
        if (record.state != TestState.InFlight) return AbandonSelection.EMPTY
        records.remove(scope)
        record.state = TestState.Abandoned
        ownerSignals.remove(scope)
        openGates.decrementAndGet()
        removeResponseDeliveryGate(scope)
        val waiters = record.waiters.values.map { waiter -> waiter to outcome }
        removeAllWaiters(record)
        val owner = if (completeOwner) record.ownerCompletion to outcome else null
        return AbandonSelection(owner, waiters)
    }

    private fun removeAllWaiters(record: TestRecord) {
        val removed = record.waiters.size
        record.waiters.clear()
        record.waiterCount.value = 0
        if (removed > 0) activeWaiters.addAndGet(-removed)
    }

    private fun removeWaiter(record: TestRecord, waiterId: Long) {
        if (record.waiters.remove(waiterId) != null) {
            activeWaiters.decrementAndGet()
            updateWaiterCount(record)
        }
    }

    private fun updateWaiterCount(record: TestRecord) {
        record.waiterCount.value = record.waiters.size
    }

    private fun removeResponseDeliveryGate(scope: String): CompletableDeferred<Unit>? =
        responseDeliveryGates.remove(scope)?.also { openGates.decrementAndGet() }

    private fun observeNextCleanupAttempt(
        reference: AtomicReference<CompletableDeferred<Unit>?>,
    ): CompletableDeferred<Unit> = CompletableDeferred<Unit>().also { observation ->
        check(reference.compareAndSet(null, observation)) { "Cleanup observation already armed." }
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
        val responseDeliveryGate: CompletableDeferred<Unit>?,
        var state: TestState = TestState.InFlight,
        var response: HttpIdempotencyResponse? = null,
        val ownerCompletion: CompletableDeferred<HttpIdempotencyResponse> = CompletableDeferred(),
        val waiters: MutableMap<Long, TestWaiter> = linkedMapOf(),
        val waiterCount: MutableStateFlow<Int> = MutableStateFlow(0),
    )

    private class TestWaiter(
        val id: Long,
        val deadline: Duration,
        val completion: CompletableDeferred<HttpIdempotencyResponse> = CompletableDeferred(),
    )

    private sealed interface TestState {
        data object InFlight: TestState
        data object Terminal: TestState
        data object Abandoned: TestState
    }

    private sealed interface ExchangeAction {
        data class Owner(val scope: String, val record: TestRecord): ExchangeAction
        data class Waiter(val scope: String, val record: TestRecord, val waiter: TestWaiter): ExchangeAction
        data class Immediate(val response: HttpIdempotencyResponse): ExchangeAction
    }

    private data class CompletionSelection(
        val owner: Pair<CompletableDeferred<HttpIdempotencyResponse>, HttpIdempotencyResponse>,
        val waiters: List<Pair<TestWaiter, HttpIdempotencyResponse>>,
    )

    private data class AbandonSelection(
        val owner: Pair<CompletableDeferred<HttpIdempotencyResponse>, HttpIdempotencyResponse>?,
        val waiters: List<Pair<TestWaiter, HttpIdempotencyResponse>>,
    ) {
        companion object {
            val EMPTY = AbandonSelection(null, emptyList())
        }
    }

    private data class PendingReset(
        val owners: List<CompletableDeferred<HttpIdempotencyResponse>>,
        val waiters: List<CompletableDeferred<HttpIdempotencyResponse>>,
        val responseDeliveries: List<CompletableDeferred<Unit>>,
        val ownerSignals: List<CompletableDeferred<Unit>>,
    )

    private companion object {
        const val DENIED_SCOPE = "denied"
        const val RESET_WAITER_COUNT = -1
    }
}
