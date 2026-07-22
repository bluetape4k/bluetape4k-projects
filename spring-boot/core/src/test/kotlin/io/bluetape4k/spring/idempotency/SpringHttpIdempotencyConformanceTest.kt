package io.bluetape4k.spring.idempotency

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.http.idempotency.BoundedWaitHttpIdempotencyAdapter
import io.bluetape4k.junit5.http.idempotency.BoundedWaitHttpIdempotencyConformanceConfig
import io.bluetape4k.junit5.http.idempotency.HttpIdempotencyQuiescence
import io.bluetape4k.junit5.http.idempotency.HttpIdempotencyRequest
import io.bluetape4k.junit5.http.idempotency.HttpIdempotencyResponse
import io.bluetape4k.junit5.http.idempotency.assertBoundedWaitHttpIdempotencyConformance
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.ServletException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.filter.OncePerRequestFilter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SpringHttpIdempotencyConformanceTest {

    @Test
    fun `Spring MockMvc satisfies bounded wait HTTP idempotency conformance`() = runSuspendIO {
        val config = conformanceConfig()
        val application = SpringFakeIdempotencyApplication(config)
        Executors.newFixedThreadPool(8).asCoroutineDispatcher().use { dispatcher ->
            val mockMvc = MockMvcBuilders.standaloneSetup(IdempotencyController(application))
                .addFilters<org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder>(
                    BoundedBodyFilter(config.maxRequestBodyBytes),
                )
                .build()
            val adapter = SpringBoundedWaitHttpIdempotencyAdapter(mockMvc, application, dispatcher, config)

            assertBoundedWaitHttpIdempotencyConformance(adapter, config)

            adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
        }
    }

    @Test
    fun `bounded body filter rejects declared and unknown length overflow before controller lookup`() {
        val maximum = 16
        val filter = BoundedBodyFilter(maximum)
        val controllerInvocations = AtomicInteger()
        val terminalServlet = object: HttpServlet() {
            override fun service(request: HttpServletRequest, response: HttpServletResponse) {
                controllerInvocations.incrementAndGet()
            }
        }

        val declaredReads = AtomicInteger()
        val declared = CountingMockRequest("x".repeat(maximum + 1).toByteArray(), declaredReads, maximum + 1L)
        val declaredResponse = MockHttpServletResponse()
        filter.doFilter(declared, declaredResponse, MockFilterChain(terminalServlet))
        declaredResponse.status shouldBeEqualTo 413
        declaredReads.get() shouldBeEqualTo 0
        controllerInvocations.get() shouldBeEqualTo 0

        val streamingReads = AtomicInteger()
        val streaming = CountingMockRequest("x".repeat(maximum + 1).toByteArray(), streamingReads, -1L)
        val streamingResponse = MockHttpServletResponse()
        filter.doFilter(streaming, streamingResponse, MockFilterChain(terminalServlet))
        streamingResponse.status shouldBeEqualTo 413
        streamingReads.get() shouldBeEqualTo maximum + 1
        controllerInvocations.get() shouldBeEqualTo 0
    }

    @Test
    fun `blocked MockMvc exchange is interrupted and caller owned threads terminate`() = runSuspendIO {
        val threadPrefix = "spring-idempotency-blocking-"
        val threadSequence = AtomicInteger()
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread.ofPlatform().name(threadPrefix + threadSequence.incrementAndGet()).unstarted(runnable)
        }
        val entered = CountDownLatch(1)
        val interrupted = AtomicInteger()
        val controller = BlockingController(entered, interrupted)
        val mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        val dispatcher = executor.asCoroutineDispatcher()
        val config = conformanceConfig().copy(scenarioTimeout = Duration.ofSeconds(1))
        val application = SpringFakeIdempotencyApplication(config)
        val adapter = SpringBoundedWaitHttpIdempotencyAdapter(mockMvc, application, dispatcher, config)
        try {
            assertFailsWith<TimeoutCancellationException> {
                withTimeout(config.scenarioTimeout.toMillis()) {
                    adapter.exchange(
                        HttpIdempotencyRequest(
                            authenticationProfile = "tenant-a-principal",
                            operation = "blocked-command",
                            resourceIdentity = "blocked-resource",
                            idempotencyKeys = listOf("blocked-key"),
                            requestBody = "{}",
                        ),
                    )
                }
            }
            entered.await(1, TimeUnit.SECONDS).shouldBeTrue()
            interrupted.get() shouldBeEqualTo 1
            adapter.resetScenario()
            adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
        } finally {
            dispatcher.close()
        }

        executor.awaitTermination(2, TimeUnit.SECONDS).shouldBeTrue()
        Thread.getAllStackTraces().keys
            .any { thread -> thread.isAlive && thread.name.startsWith(threadPrefix) }
            .shouldBeFalse()
    }

    private fun conformanceConfig(maxRequestBodyBytes: Int = 64): BoundedWaitHttpIdempotencyConformanceConfig =
        BoundedWaitHttpIdempotencyConformanceConfig(
            waitTimeout = Duration.ofSeconds(2),
            scenarioTimeout = Duration.ofSeconds(15),
            // Three fan-in keys must leave room for overflow probes on the bounded 8-thread pool.
            maxWaitersPerKey = 1,
            retention = Duration.ofHours(1),
            inFlightRetryAfter = Duration.ofSeconds(1),
            overflowRetryAfter = Duration.ofSeconds(2),
            maxIdempotencyKeyBytes = 255,
            maxRequestBodyBytes = maxRequestBodyBytes,
            maxReplayBodyBytes = 64 * 1024,
            maxReplayHeaderNames = 8,
            maxReplayValuesPerHeader = 4,
            maxReplayHeaderValueBytes = 4 * 1024,
            maxReplayHeaderBytes = 16 * 1024,
            replayHeaderAllowlist = buildSet {
                add("etag")
                DENIED_REPLAY_HEADERS.forEach(::add)
                add("x-hop")
                add("x-credential")
                add("x-secret")
                add("session-token")
                add("client-api-key")
                repeat(8) { index -> add("x-safe-$index") }
            },
        )
}

private class SpringBoundedWaitHttpIdempotencyAdapter(
    private val mockMvc: MockMvc,
    private val application: SpringFakeIdempotencyApplication,
    private val dispatcher: CoroutineDispatcher,
    private val config: BoundedWaitHttpIdempotencyConformanceConfig,
): BoundedWaitHttpIdempotencyAdapter {

    override suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse =
        runInterruptible(dispatcher) {
            val response = mockMvc.performInterruptibly(
                post("/commands/{resourceId}", request.resourceIdentity)
                    .header(TEST_AUTH_PROFILE, request.authenticationProfile)
                    .header(TEST_OPERATION, request.operation)
                    .header(IDEMPOTENCY_KEY, *request.idempotencyKeys.toTypedArray())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request.requestBody),
            ).response
            val semanticHeaders = response.getHeader(TEST_RESPONSE_HEADERS).orEmpty()
                .split(',')
                .map { name -> name.trim().lowercase() }
                .filter(String::isNotEmpty)
                .toSet()
            val headers = semanticHeaders
                .filter { name ->
                    name == CONTENT_TYPE || name == REPLAYED || name == RETRY_AFTER ||
                            name in config.replayHeaderAllowlist
                }
                .associateWith { name -> response.getHeaders(name) }
            HttpIdempotencyResponse(
                statusCode = response.status,
                body = response.contentAsString,
                headers = headers,
                problemCode = response.getHeader(PROBLEM_CODE),
            )
        }

    override suspend fun awaitOwnerStarted(request: HttpIdempotencyRequest) = application.awaitOwnerStarted(request)

    override suspend fun awaitWaiterCount(request: HttpIdempotencyRequest, expected: Int) =
        application.awaitWaiterCount(request, expected)

    override suspend fun completeOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse) =
        application.completeOwner(request, outcome)

    override suspend fun holdOwnerResponseDelivery(request: HttpIdempotencyRequest) =
        application.holdOwnerResponseDelivery(request)

    override suspend fun releaseOwnerResponseDelivery(request: HttpIdempotencyRequest) =
        application.releaseOwnerResponseDelivery(request)

    override suspend fun abandonOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse) =
        application.abandonOwner(request, outcome)

    override suspend fun advanceTimeBy(duration: Duration) = application.advanceTimeBy(duration)

    override suspend fun resetScenario() = application.resetScenario()

    override fun sideEffectCount(request: HttpIdempotencyRequest): Int = application.sideEffectCount(request)

    override fun quiescence(): HttpIdempotencyQuiescence = application.quiescence()
}

@Controller
private class IdempotencyController(
    private val application: SpringFakeIdempotencyApplication,
    private val authProfiles: AuthProfiles = AuthProfiles(),
) {

    @PostMapping("/commands/{resourceId}")
    fun command(
        @RequestHeader(TEST_AUTH_PROFILE, required = false) authenticationProfile: String?,
        @RequestHeader(TEST_OPERATION, required = false, defaultValue = "") operation: String,
        @RequestHeader(IDEMPOTENCY_KEY, required = false) keys: List<String>?,
        @PathVariable resourceId: String,
        @RequestBody body: String,
    ): ResponseEntity<String> {
        val principal = authProfiles.resolve(authenticationProfile)
            ?: return idempotencyResponse(401, "authentication_required").toResponseEntity()
        if (!principal.canWrite) return idempotencyResponse(403, "forbidden").toResponseEntity()
        val request = HttpIdempotencyRequest(
            authenticationProfile = checkNotNull(authenticationProfile),
            operation = operation,
            resourceIdentity = resourceId,
            idempotencyKeys = keys.orEmpty(),
            requestBody = body,
        )
        return application.execute(principal.tenant, request).toResponseEntity()
    }
}

private class AuthProfiles {
    fun resolve(profile: String?): Principal? = when (profile) {
        "tenant-a-principal" -> Principal("tenant-a", true)
        "tenant-b-principal" -> Principal("tenant-b", true)
        "tenant-a-read-only" -> Principal("tenant-a", false)
        else -> null
    }
}

private class Principal(val tenant: String, val canWrite: Boolean)

private class SpringFakeIdempotencyApplication(
    private val config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    private val lock = ReentrantLock()
    private val records = mutableMapOf<String, Record>()
    private val ownerSignals = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val responseDeliveryGates = mutableMapOf<String, CompletableFuture<Unit>>()
    private val sideEffects = mutableMapOf<String, Int>()
    private val waiterSequence = AtomicLong()
    private var virtualNow: Duration = Duration.ZERO
    private var activeWaiters = 0
    private var openGates = 0

    val lookupCount = AtomicInteger()

    fun execute(tenant: String, request: HttpIdempotencyRequest): HttpIdempotencyResponse {
        validateIngress(request)?.let { return it }
        lookupCount.incrementAndGet()
        val scope = scope(tenant, request)
        val fingerprint = canonicalPayloadOrNull(request.requestBody)
            ?: return idempotencyResponse(400, "invalid_idempotency_request")
        val action = lock.withLock { decideExchange(scope, fingerprint) }
        return try {
            when (action) {
                is Action.Owner -> awaitOwner(action)
                is Action.Waiter -> awaitWaiter(action)
                is Action.Immediate -> action.response
            }
        } catch (failure: ExecutionException) {
            throw checkNotNull(failure.cause)
        }
    }

    suspend fun awaitOwnerStarted(request: HttpIdempotencyRequest) {
        val signal = lock.withLock {
            val scope = scopeForAuthorizedRequest(request)
            val current = records[scope]
            if (current?.state == RecordState.Terminal && virtualNow >= checkNotNull(current.expiresAt)) {
                records.remove(scope)
                ownerSignals.remove(scope)
            }
            ownerSignals.getOrPut(scope) { CompletableDeferred() }
        }
        signal.await()
    }

    suspend fun awaitWaiterCount(request: HttpIdempotencyRequest, expected: Int) {
        require(expected >= 0) { "expected must not be negative." }
        val record = lock.withLock { records[scopeForAuthorizedRequest(request)] }
            ?: error("Owner was not started; values redacted.")
        val observed = record.waiterCount.first { count -> count == expected || count == RESET_WAITER_COUNT }
        check(observed != RESET_WAITER_COUNT) { "Scenario reset before waiter count was observed." }
    }

    fun completeOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse) {
        val replayable = replayableOutcomeOrNull(outcome)
        if (replayable == null) {
            rejectUnsafeSnapshot(request)
            return
        }
        val completion = lock.withLock {
            val record = records[scopeForAuthorizedRequest(request)]
                ?: error("Owner was not started; values redacted.")
            check(record.state == RecordState.InFlight) { "Owner is not in flight; values redacted." }
            record.state = RecordState.Terminal
            record.response = replayable
            record.expiresAt = virtualNow.plus(config.retention)
            openGates--
            val waiters = record.waiters.values.map { waiter ->
                waiter to if (virtualNow >= waiter.deadline) timeoutResponse() else replayable.withReplayFlag(true)
            }
            removeAllWaiters(record)
            Completion(record.owner to replayable.withReplayFlag(false), waiters)
        }
        completion.owner.first.complete(completion.owner.second)
        completion.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
    }

    fun holdOwnerResponseDelivery(request: HttpIdempotencyRequest) {
        lock.withLock {
            val scope = scopeForAuthorizedRequest(request)
            check(scope !in records) { "Owner already started; values redacted." }
            check(scope !in responseDeliveryGates) { "Response delivery already held; values redacted." }
            responseDeliveryGates[scope] = CompletableFuture()
            openGates++
        }
    }

    fun releaseOwnerResponseDelivery(request: HttpIdempotencyRequest) {
        lock.withLock { removeResponseDeliveryGate(scopeForAuthorizedRequest(request)) }?.complete(Unit)
    }

    fun abandonOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse) {
        val abandoned = lock.withLock { abandonRecord(scopeForAuthorizedRequest(request), outcome, true) }
        abandoned.owner?.let { (owner, response) -> owner.complete(response) }
        abandoned.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
    }

    suspend fun advanceTimeBy(duration: Duration) {
        require(!duration.isNegative) { "duration must not be negative." }
        val timedOut = lock.withLock {
            virtualNow = virtualNow.plus(duration)
            records.values.flatMap { record ->
                if (record.state != RecordState.InFlight) return@flatMap emptyList()
                record.waiters.values.filter { waiter -> virtualNow >= waiter.deadline }.also { expired ->
                    expired.forEach { waiter -> removeWaiter(record, waiter.id) }
                }
            }
        }
        timedOut.forEach { waiter -> waiter.completion.complete(timeoutResponse()) }
        yield()
    }

    suspend fun resetScenario() {
        yield()
        val reset = lock.withLock {
            Reset(
                owners = records.values.map(Record::owner),
                waiters = records.values.flatMap { record -> record.waiters.values.map(Waiter::completion) },
                deliveries = responseDeliveryGates.values.toList(),
                signals = ownerSignals.values.toList(),
            ).also {
                records.values.forEach { record -> record.waiterCount.value = RESET_WAITER_COUNT }
                records.clear()
                ownerSignals.clear()
                responseDeliveryGates.clear()
                sideEffects.clear()
                activeWaiters = 0
                openGates = 0
                virtualNow = Duration.ZERO
                lookupCount.set(0)
            }
        }
        val response = idempotencyResponse(503, "temporarily_unavailable")
        reset.owners.forEach { completion -> completion.complete(response) }
        reset.waiters.forEach { completion -> completion.complete(response) }
        reset.deliveries.forEach { completion -> completion.complete(Unit) }
        reset.signals.forEach { signal -> signal.cancel() }
    }

    fun sideEffectCount(request: HttpIdempotencyRequest): Int {
        if (request.idempotencyKeys.size != 1) return 0
        val tenant = when (request.authenticationProfile) {
            "tenant-a-principal" -> "tenant-a"
            "tenant-b-principal" -> "tenant-b"
            else -> return 0
        }
        return lock.withLock { sideEffects[scope(tenant, request)] ?: 0 }
    }

    fun quiescence(): HttpIdempotencyQuiescence = lock.withLock {
        HttpIdempotencyQuiescence(activeWaiters, openGates, 0)
    }

    private fun decideExchange(scope: String, fingerprint: String): Action {
        val existing = records[scope]
        if (existing == null) return createOwner(scope, fingerprint)
        if (existing.state == RecordState.Terminal && virtualNow >= checkNotNull(existing.expiresAt)) {
            records.remove(scope)
            ownerSignals.remove(scope)
            return createOwner(scope, fingerprint)
        }
        if (existing.fingerprint != fingerprint) {
            return Action.Immediate(idempotencyResponse(409, "idempotency_key_reused"))
        }
        return when (existing.state) {
            RecordState.InFlight -> registerWaiterOrReject(scope, existing)
            RecordState.Terminal -> Action.Immediate(checkNotNull(existing.response).withReplayFlag(true))
            RecordState.Abandoned -> error("Abandoned records must not remain visible.")
        }
    }

    private fun createOwner(scope: String, fingerprint: String): Action.Owner {
        val record = Record(fingerprint, responseDeliveryGates[scope])
        records[scope] = record
        sideEffects[scope] = (sideEffects[scope] ?: 0) + 1
        openGates++
        ownerSignals.getOrPut(scope) { CompletableDeferred() }.complete(Unit)
        return Action.Owner(scope, record)
    }

    private fun registerWaiterOrReject(scope: String, record: Record): Action {
        if (record.waiters.size >= config.maxWaitersPerKey) {
            return Action.Immediate(idempotencyResponse(429, "idempotency_waiters_exceeded", config.overflowRetryAfter))
        }
        val waiter = Waiter(waiterSequence.incrementAndGet(), virtualNow.plus(config.waitTimeout))
        record.waiters[waiter.id] = waiter
        record.waiterCount.value = record.waiters.size
        activeWaiters++
        return Action.Waiter(scope, record, waiter)
    }

    private fun awaitOwner(action: Action.Owner): HttpIdempotencyResponse = try {
        action.record.owner.get().also { action.record.delivery?.get() }
    } catch (interrupted: InterruptedException) {
        val abandoned = lock.withLock {
            val current = records[action.scope]
            if (current === action.record && current.state == RecordState.InFlight) {
                abandonRecord(action.scope, idempotencyResponse(503, "temporarily_unavailable"), false)
            } else {
                removeResponseDeliveryGate(action.scope)
                Abandoned.EMPTY
            }
        }
        abandoned.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
        throw interrupted
    }

    private fun awaitWaiter(action: Action.Waiter): HttpIdempotencyResponse = try {
        action.waiter.completion.get()
    } finally {
        lock.withLock {
            val current = records[action.scope]
            if (current === action.record) removeWaiter(current, action.waiter.id)
        }
    }

    private fun abandonRecord(scope: String, outcome: HttpIdempotencyResponse, completeOwner: Boolean): Abandoned {
        val record = records[scope] ?: return Abandoned.EMPTY
        if (record.state != RecordState.InFlight) return Abandoned.EMPTY
        records.remove(scope)
        record.state = RecordState.Abandoned
        ownerSignals.remove(scope)
        openGates--
        removeResponseDeliveryGate(scope)
        val waiters = record.waiters.values.map { waiter -> waiter to outcome }
        removeAllWaiters(record)
        return Abandoned(if (completeOwner) record.owner to outcome else null, waiters)
    }

    private fun rejectUnsafeSnapshot(request: HttpIdempotencyRequest) {
        val completion = lock.withLock {
            val scope = scopeForAuthorizedRequest(request)
            val record = records.remove(scope) ?: error("Owner was not started; values redacted.")
            ownerSignals.remove(scope)
            openGates--
            removeResponseDeliveryGate(scope)
            sideEffects[scope] = (sideEffects[scope] ?: 1) - 1
            val response = idempotencyResponse(500, "idempotency_snapshot_rejected")
            val waiters = record.waiters.values.map { waiter -> waiter to response }
            removeAllWaiters(record)
            Completion(record.owner to response, waiters)
        }
        completion.owner.first.complete(completion.owner.second)
        completion.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
    }

    private fun validateIngress(request: HttpIdempotencyRequest): HttpIdempotencyResponse? {
        if (request.idempotencyKeys.size != 1) return idempotencyResponse(400, "invalid_idempotency_request")
        val key = request.idempotencyKeys.single()
        if (key.toByteArray().size > config.maxIdempotencyKeyBytes || key.isEmpty() ||
            key.any { character -> character.code !in 0x21..0x7e } || canonicalPayloadOrNull(request.requestBody) == null
        ) return idempotencyResponse(400, "invalid_idempotency_request")
        return null
    }

    private fun replayableOutcomeOrNull(outcome: HttpIdempotencyResponse): HttpIdempotencyResponse? {
        if (outcome.body.toByteArray().size > config.maxReplayBodyBytes) return null
        val connectionNominated = outcome.headers["connection"].orEmpty()
            .flatMap { value -> value.split(',') }
            .map { name -> name.trim().lowercase() }
            .filter(String::isNotEmpty)
            .toSet()
        val headers = outcome.headers.filterKeys { name ->
            (name == CONTENT_TYPE || name in config.replayHeaderAllowlist) &&
                    !isReplayHeaderDenied(name) && name !in connectionNominated
        }
        if (headers.size > config.maxReplayHeaderNames) return null
        var aggregate = 0L
        headers.forEach { (name, values) ->
            if (values.size > config.maxReplayValuesPerHeader) return null
            aggregate += name.toByteArray().size
            if (aggregate > config.maxReplayHeaderBytes) return null
            values.forEach { value ->
                val bytes = value.toByteArray().size
                if (bytes > config.maxReplayHeaderValueBytes) return null
                aggregate += bytes
                if (aggregate > config.maxReplayHeaderBytes) return null
            }
        }
        return outcome.copy(headers = headers)
    }

    private fun scopeForAuthorizedRequest(request: HttpIdempotencyRequest): String = scope(
        when (request.authenticationProfile) {
            "tenant-a-principal" -> "tenant-a"
            "tenant-b-principal" -> "tenant-b"
            else -> error("Authentication must complete before idempotency lookup; values redacted.")
        },
        request,
    )

    private fun scope(tenant: String, request: HttpIdempotencyRequest): String =
        listOf(tenant, digest(request.operation), digest(request.resourceIdentity), digest(request.idempotencyKeys.single()))
            .joinToString("|")

    private fun removeAllWaiters(record: Record) {
        activeWaiters -= record.waiters.size
        record.waiters.clear()
        record.waiterCount.value = 0
    }

    private fun removeWaiter(record: Record, id: Long) {
        if (record.waiters.remove(id) != null) {
            activeWaiters--
            record.waiterCount.value = record.waiters.size
        }
    }

    private fun removeResponseDeliveryGate(scope: String): CompletableFuture<Unit>? =
        responseDeliveryGates.remove(scope)?.also { openGates-- }

    private fun timeoutResponse(): HttpIdempotencyResponse =
        idempotencyResponse(409, "idempotency_in_flight", config.inFlightRetryAfter)

    private class Record(
        val fingerprint: String,
        val delivery: CompletableFuture<Unit>?,
        var state: RecordState = RecordState.InFlight,
        var response: HttpIdempotencyResponse? = null,
        var expiresAt: Duration? = null,
        val owner: CompletableFuture<HttpIdempotencyResponse> = CompletableFuture(),
        val waiters: MutableMap<Long, Waiter> = linkedMapOf(),
        val waiterCount: MutableStateFlow<Int> = MutableStateFlow(0),
    )

    private class Waiter(
        val id: Long,
        val deadline: Duration,
        val completion: CompletableFuture<HttpIdempotencyResponse> = CompletableFuture(),
    )

    private enum class RecordState { InFlight, Terminal, Abandoned }

    private sealed interface Action {
        class Owner(val scope: String, val record: Record): Action
        class Waiter(val scope: String, val record: Record, val waiter: SpringFakeIdempotencyApplication.Waiter): Action
        class Immediate(val response: HttpIdempotencyResponse): Action
    }

    private class Completion(
        val owner: Pair<CompletableFuture<HttpIdempotencyResponse>, HttpIdempotencyResponse>,
        val waiters: List<Pair<Waiter, HttpIdempotencyResponse>>,
    )

    private class Abandoned(
        val owner: Pair<CompletableFuture<HttpIdempotencyResponse>, HttpIdempotencyResponse>?,
        val waiters: List<Pair<Waiter, HttpIdempotencyResponse>>,
    ) {
        companion object { val EMPTY = Abandoned(null, emptyList()) }
    }

    private class Reset(
        val owners: List<CompletableFuture<HttpIdempotencyResponse>>,
        val waiters: List<CompletableFuture<HttpIdempotencyResponse>>,
        val deliveries: List<CompletableFuture<Unit>>,
        val signals: List<CompletableDeferred<Unit>>,
    )
}

private class BoundedBodyFilter(private val maxBytes: Int): OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.contentLengthLong > maxBytes) {
            response.writeProblem(413, "idempotency_request_too_large")
            return
        }
        val bytes = readAtMost(request, maxBytes + 1)
        if (bytes.size > maxBytes) {
            response.writeProblem(413, "idempotency_request_too_large")
            return
        }
        val body = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            response.writeProblem(400, "invalid_idempotency_request")
            return
        }
        filterChain.doFilter(BoundedBodyRequest(request, body.toByteArray(Charsets.UTF_8)), response)
    }

    private fun readAtMost(request: HttpServletRequest, limit: Int): ByteArray {
        val output = ByteArrayOutputStream(limit)
        val buffer = ByteArray(minOf(1024, limit))
        request.inputStream.use { input ->
            while (output.size() < limit) {
                val read = input.read(buffer, 0, minOf(buffer.size, limit - output.size()))
                if (read < 0) break
                output.write(buffer, 0, read)
            }
        }
        return output.toByteArray()
    }
}

private class BoundedBodyRequest(request: HttpServletRequest, private val body: ByteArray):
    HttpServletRequestWrapper(request) {
    override fun getContentLength(): Int = body.size
    override fun getContentLengthLong(): Long = body.size.toLong()
    override fun getInputStream(): ServletInputStream = ByteArrayServletInputStream(body)
}

private class ByteArrayServletInputStream(body: ByteArray): ServletInputStream() {
    private val delegate = ByteArrayInputStream(body)

    override fun read(): Int = delegate.read()
    override fun read(target: ByteArray, offset: Int, length: Int): Int = delegate.read(target, offset, length)
    override fun isFinished(): Boolean = delegate.available() == 0
    override fun isReady(): Boolean = true
    override fun setReadListener(readListener: ReadListener?) = Unit
}

private class CountingMockRequest(
    private val body: ByteArray,
    private val readCount: AtomicInteger,
    private val declaredLength: Long,
): MockHttpServletRequest("POST", "/commands/widget-1") {
    override fun getContentLength(): Int = declaredLength.toInt()
    override fun getContentLengthLong(): Long = declaredLength
    override fun getInputStream(): ServletInputStream = object: ServletInputStream() {
        private val delegate = ByteArrayInputStream(body)
        override fun read(): Int = delegate.read().also { value -> if (value >= 0) readCount.incrementAndGet() }
        override fun read(target: ByteArray, offset: Int, length: Int): Int =
            delegate.read(target, offset, length).also { count -> if (count > 0) readCount.addAndGet(count) }
        override fun isFinished(): Boolean = delegate.available() == 0
        override fun isReady(): Boolean = true
        override fun setReadListener(readListener: ReadListener?) = Unit
    }
}

@Controller
private class BlockingController(
    private val entered: CountDownLatch,
    private val interrupted: AtomicInteger,
) {
    @PostMapping("/commands/{resourceId}")
    fun blocked(): ResponseEntity<String> = try {
        entered.countDown()
        CountDownLatch(1).await()
        ResponseEntity.ok("unreachable")
    } catch (failure: InterruptedException) {
        interrupted.incrementAndGet()
        throw failure
    }
}

private fun HttpIdempotencyResponse.toResponseEntity(): ResponseEntity<String> {
    val headers = HttpHeaders()
    headers.add(TEST_RESPONSE_HEADERS, this.headers.keys.joinToString(","))
    this.headers.forEach { (name, values) -> values.forEach { value -> headers.add(name, value) } }
    problemCode?.let { code -> headers.add(PROBLEM_CODE, code) }
    return ResponseEntity.status(statusCode).headers(headers).body(body)
}

private fun HttpServletResponse.writeProblem(status: Int, code: String) {
    this.status = status
    contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
    setHeader(PROBLEM_CODE, code)
    setHeader(TEST_RESPONSE_HEADERS, CONTENT_TYPE)
    writer.write("{\"code\":\"$code\"}")
}

private fun HttpIdempotencyResponse.withReplayFlag(replayed: Boolean): HttpIdempotencyResponse =
    copy(headers = headers + (REPLAYED to listOf(replayed.toString())))

private fun idempotencyResponse(
    status: Int,
    problemCode: String,
    retryAfter: Duration? = null,
): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = status,
    body = "{\"code\":\"$problemCode\"}",
    headers = buildMap {
        put(CONTENT_TYPE, listOf(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
        retryAfter?.let { put(RETRY_AFTER, listOf(it.seconds.toString())) }
    },
    problemCode = problemCode,
)

private fun canonicalPayloadOrNull(body: String): String? = try {
    canonicalJson(JSON_MAPPER.readTree(body))
} catch (_: Exception) {
    null
}

private fun canonicalJson(node: JsonNode): String = when {
    node.isObject -> node.properties().asSequence().map { entry -> entry.key to canonicalJson(entry.value) }
        .sortedBy(Pair<String, String>::first)
        .joinToString(prefix = "{", postfix = "}") { (name, value) ->
            "${JSON_MAPPER.writeValueAsString(name)}:$value"
        }
    node.isArray -> node.elements().asSequence().map(::canonicalJson).joinToString(prefix = "[", postfix = "]")
    node.isTextual -> JSON_MAPPER.writeValueAsString(requireValidUtf8(node.textValue()))
    node.isNumber -> node.decimalValue().stripTrailingZeros().toCanonicalNumber()
    node.isBoolean -> node.booleanValue().toString()
    node.isNull -> "null"
    else -> error("Unsupported JSON node.")
}

private fun requireValidUtf8(value: String): String = value.also {
    Charsets.UTF_8.newEncoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .encode(java.nio.CharBuffer.wrap(value))
}

private fun BigDecimal.toCanonicalNumber(): String = if (scale() < 0) setScale(0).toPlainString() else toPlainString()

private fun digest(value: String): String = HexFormat.of().formatHex(
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray()),
)

private fun isReplayHeaderDenied(name: String): Boolean =
    name in DENIED_REPLAY_HEADERS || name.contains("credential") || name.contains("secret") ||
            name.endsWith("-token") || name.endsWith("-api-key")

private fun MockMvc.performInterruptibly(
    request: org.springframework.test.web.servlet.RequestBuilder,
): org.springframework.test.web.servlet.MvcResult = try {
    perform(request).andReturn()
} catch (failure: ServletException) {
    val interrupted = generateSequence<Throwable>(failure) { current -> current.cause }
        .filterIsInstance<InterruptedException>()
        .firstOrNull()
    if (interrupted != null) throw interrupted
    throw failure
}

private val JSON_MAPPER = ObjectMapper()
    .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
    .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
    .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)

private val DENIED_REPLAY_HEADERS = setOf(
    "authentication-info",
    "authorization",
    "connection",
    "cookie",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authentication-info",
    "proxy-authorization",
    "set-cookie",
    "te",
    "trailer",
    "transfer-encoding",
    "upgrade",
    "www-authenticate",
    "x-api-key",
)

private const val TEST_AUTH_PROFILE = "X-Test-Auth-Profile"
private const val TEST_OPERATION = "X-Test-Operation"
private const val IDEMPOTENCY_KEY = "Idempotency-Key"
private const val PROBLEM_CODE = "X-Problem-Code"
private const val TEST_RESPONSE_HEADERS = "X-Test-Response-Headers"
private const val CONTENT_TYPE = "content-type"
private const val REPLAYED = "idempotency-replayed"
private const val RETRY_AFTER = "retry-after"
private const val RESET_WAITER_COUNT = -1
