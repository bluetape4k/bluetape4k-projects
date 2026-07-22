package io.bluetape4k.ktor.testing.idempotency

import io.bluetape4k.assertions.invoking
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.http.idempotency.BoundedWaitHttpIdempotencyAdapter
import io.bluetape4k.junit5.http.idempotency.BoundedWaitHttpIdempotencyConformanceConfig
import io.bluetape4k.junit5.http.idempotency.HttpIdempotencyQuiescence
import io.bluetape4k.junit5.http.idempotency.HttpIdempotencyRequest
import io.bluetape4k.junit5.http.idempotency.HttpIdempotencyResponse
import io.bluetape4k.junit5.http.idempotency.assertBoundedWaitHttpIdempotencyConformance
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.contentLength
import io.ktor.server.request.header
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.io.readByteArray
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class KtorHttpIdempotencyConformanceTest {

    @Test
    fun `Ktor testApplication satisfies bounded wait HTTP idempotency conformance`() = testApplication {
        val config = conformanceConfig()
        val fakeApplication = KtorFakeIdempotencyApplication(config)
        application { fakeApplication.installRoutes(this) }

        val adapter = KtorBoundedWaitHttpIdempotencyAdapter(client, fakeApplication, config)
        assertBoundedWaitHttpIdempotencyConformance(adapter, config)

        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }

    @Test
    fun `Ktor ingress rejects transport-legal malformed keys in the application`() = testApplication {
        val config = conformanceConfig()
        val fakeApplication = KtorFakeIdempotencyApplication(config)
        application { fakeApplication.installRoutes(this) }

        val invalidValues = listOf(listOf("duplicate", "duplicate"), listOf(" "), listOf("\t"), listOf("é"))
        invalidValues.forEachIndexed { index, values ->
            val before = fakeApplication.routeEntryCount
            val exchangeId = 1_000L + index
            fakeApplication.registerExchangeCancellation(exchangeId)
            val response = client.post("/commands/widget-1") {
                header(TEST_AUTH_PROFILE, "tenant-a-principal")
                header(TEST_OPERATION, "create-widget")
                header(TEST_EXCHANGE_ID, exchangeId)
                headers { values.forEach { value -> append(IDEMPOTENCY_KEY, value) } }
                setBody("{\"name\":\"sample\"}")
            }

            response.status shouldBeEqualTo HttpStatusCode.BadRequest
            fakeApplication.routeEntryCount shouldBeEqualTo before + 1
        }

    }

    @Test
    fun `Ktor header builder rejects C0 idempotency key before route entry`() {
        invoking {
            Headers.build {
                append(IDEMPOTENCY_KEY, "bad\u0001key")
            }
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `Ktor bounded reader rejects declared and streaming overflow before lookup`() = testApplication {
        val config = conformanceConfig(maxRequestBodyBytes = 16)
        val fakeApplication = KtorFakeIdempotencyApplication(config)
        application { fakeApplication.installRoutes(this) }

        fakeApplication.registerExchangeCancellation(2_000L)
        val declared = client.post("/commands/widget-1") {
            header(TEST_AUTH_PROFILE, "tenant-a-principal")
            header(TEST_OPERATION, "create-widget")
            header(IDEMPOTENCY_KEY, "declared-overflow")
            header(TEST_EXCHANGE_ID, 2_000L)
            header(HttpHeaders.ContentLength, "17")
            setBody("x".repeat(17))
        }
        declared.status shouldBeEqualTo HttpStatusCode.PayloadTooLarge
        fakeApplication.lastBodyReadBytes shouldBeEqualTo 0

        fakeApplication.registerExchangeCancellation(2_001L)
        val streaming = client.post("/commands/widget-1") {
            header(TEST_AUTH_PROFILE, "tenant-a-principal")
            header(TEST_OPERATION, "create-widget")
            header(IDEMPOTENCY_KEY, "streaming-overflow")
            header(TEST_EXCHANGE_ID, 2_001L)
            setBody(object: OutgoingContent.WriteChannelContent() {
                override val contentType: ContentType = ContentType.Application.Json
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    channel.writeFully("x".repeat(17).toByteArray())
                }
            })
        }
        streaming.status shouldBeEqualTo HttpStatusCode.PayloadTooLarge
        fakeApplication.lastBodyReadBytes shouldBeEqualTo 17
        fakeApplication.lookupCount shouldBeEqualTo 0
        fakeApplication.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }

    private fun conformanceConfig(maxRequestBodyBytes: Int = 64): BoundedWaitHttpIdempotencyConformanceConfig =
        BoundedWaitHttpIdempotencyConformanceConfig(
            waitTimeout = Duration.ofSeconds(2),
            scenarioTimeout = Duration.ofSeconds(15),
            maxWaitersPerKey = 2,
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
                add("authorization")
                add("cookie")
                add("set-cookie")
                add("proxy-authorization")
                add("www-authenticate")
                add("connection")
                add("x-hop")
                add("x-api-key")
                add("authentication-info")
                add("proxy-authenticate")
                add("proxy-authentication-info")
                add("keep-alive")
                add("te")
                add("trailer")
                add("transfer-encoding")
                add("upgrade")
                add("x-credential")
                add("x-secret")
                add("session-token")
                add("client-api-key")
                repeat(8) { index -> add("x-safe-$index") }
            },
        )
}

private class KtorBoundedWaitHttpIdempotencyAdapter(
    private val client: HttpClient,
    private val application: KtorFakeIdempotencyApplication,
    private val config: BoundedWaitHttpIdempotencyConformanceConfig,
): BoundedWaitHttpIdempotencyAdapter {
    private val exchangeSequence = AtomicLong()

    @OptIn(InternalCoroutinesApi::class)
    override suspend fun exchange(request: HttpIdempotencyRequest): HttpIdempotencyResponse {
        val exchangeId = exchangeSequence.incrementAndGet()
        val cancellation = application.registerExchangeCancellation(exchangeId)
        // Wake the exact server exchange as soon as cancellation begins. Suspending cleanup stays
        // in the structured catch path below; completion callbacks must never block an event loop.
        val cancellationHandle = currentCoroutineContext()[Job]?.invokeOnCompletion(
            onCancelling = true,
            invokeImmediately = true,
        ) { cause ->
            if (cause is CancellationException) {
                cancellation.signal.complete(Unit)
            }
        }
        val response = try {
            client.post("/commands/${request.resourceIdentity}") {
                header(TEST_AUTH_PROFILE, request.authenticationProfile)
                header(TEST_OPERATION, request.operation)
                header(TEST_EXCHANGE_ID, exchangeId)
                headers { request.idempotencyKeys.forEach { value -> append(IDEMPOTENCY_KEY, value) } }
                setBody(request.requestBody)
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { application.cancelExchange(exchangeId) }
            throw cancelled
        } finally {
            cancellationHandle?.dispose()
        }
        val semanticHeaderNames = response.headers[TEST_RESPONSE_HEADERS].orEmpty()
            .split(',')
            .map { name -> name.trim().lowercase() }
            .filter(String::isNotEmpty)
            .toSet()
        val visibleHeaders = response.headers.entries()
            .filter { (name, _) ->
                val normalized = name.lowercase()
                normalized in semanticHeaderNames &&
                        (normalized == "content-type" || normalized == "idempotency-replayed" ||
                                normalized == "retry-after" || normalized in config.replayHeaderAllowlist)
            }
            .associate { (name, values) -> name to values }
        return HttpIdempotencyResponse(
            statusCode = response.status.value,
            body = response.bodyAsText(),
            headers = visibleHeaders,
            problemCode = response.headers[PROBLEM_CODE],
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

private class KtorFakeIdempotencyApplication(
    private val config: BoundedWaitHttpIdempotencyConformanceConfig,
) {
    private val mutex = Mutex()
    private val records = mutableMapOf<String, Record>()
    private val ownerSignals = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val responseDeliveryGates = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val exchangeCancellations = ConcurrentHashMap<Long, ExchangeCancellation>()
    private val exchangeActions = mutableMapOf<Long, ExchangeAction>()
    private val sideEffects = ConcurrentHashMap<String, AtomicInteger>()
    private val activeWaiters = AtomicInteger()
    private val openGates = AtomicInteger()
    private val waiterSequence = AtomicLong()
    private var virtualNow: Duration = Duration.ZERO

    var routeEntryCount: Int = 0
        private set
    var lastBodyReadBytes: Int = 0
        private set
    var lookupCount: Int = 0
        private set

    fun installRoutes(application: Application) {
        application.routing {
            post("/commands/{resourceId}") {
                routeEntryCount++
                val exchangeId = call.request.header(TEST_EXCHANGE_ID)?.toLongOrNull()
                    ?: return@post call.respond(idempotencyResponse(400, "invalid_idempotency_request"))
                try {
                    val profile = call.request.header(TEST_AUTH_PROFILE)
                    val tenant = when (profile) {
                        "tenant-a-principal" -> "tenant-a"
                        "tenant-b-principal" -> "tenant-b"
                        "unauthenticated", null -> return@post call.respond(idempotencyResponse(401, "authentication_required"))
                        else -> return@post call.respond(idempotencyResponse(403, "forbidden"))
                    }
                    val body = when (val bounded = call.receiveBoundedUtf8(config.maxRequestBodyBytes)) {
                        is BoundedBodyRead.Value -> bounded.value
                        BoundedBodyRead.TooLarge -> return@post call.respond(idempotencyResponse(413, "idempotency_request_too_large"))
                        BoundedBodyRead.Malformed -> return@post call.respond(idempotencyResponse(400, "invalid_idempotency_request"))
                    }
                    val request = HttpIdempotencyRequest(
                        authenticationProfile = checkNotNull(profile),
                        operation = call.request.header(TEST_OPERATION).orEmpty(),
                        resourceIdentity = call.parameters["resourceId"].orEmpty(),
                        idempotencyKeys = call.request.headers.getAll(IDEMPOTENCY_KEY).orEmpty()
                            .flatMap { value -> value.split(',') },
                        requestBody = body,
                    )
                    call.respond(execute(exchangeId, tenant, request))
                } finally {
                    unregisterExchangeCancellation(exchangeId)
                }
            }
        }
    }

    fun registerExchangeCancellation(exchangeId: Long): ExchangeCancellation = ExchangeCancellation().also {
        check(exchangeCancellations.putIfAbsent(exchangeId, it) == null) { "Exchange id already registered." }
    }

    fun unregisterExchangeCancellation(exchangeId: Long) {
        exchangeCancellations.remove(exchangeId)
    }

    suspend fun cancelExchange(exchangeId: Long) {
        val selection = withContext(NonCancellable) {
            mutex.withLock {
                when (val action = exchangeActions.remove(exchangeId)) {
                    is ExchangeAction.Owner -> {
                        val current = records[action.scope]
                        if (current === action.record && current.state == RecordState.InFlight) {
                            CancelSelection(
                                abandon = abandonRecord(
                                    action.scope,
                                    idempotencyResponse(503, "temporarily_unavailable"),
                                    completeOwner = false,
                                ),
                            )
                        } else {
                            CancelSelection(deliveryGate = removeResponseDeliveryGate(action.scope))
                        }
                    }
                    is ExchangeAction.Waiter -> {
                        val current = records[action.scope]
                        if (current === action.record) removeWaiter(current, action.waiter.id)
                        CancelSelection(waiter = action.waiter)
                    }
                    is ExchangeAction.Immediate, null -> CancelSelection.EMPTY
                }
            }
        }
        selection.abandon.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
        selection.waiter?.completion?.complete(idempotencyResponse(503, "temporarily_unavailable"))
        selection.deliveryGate?.complete(Unit)
    }

    suspend fun awaitOwnerStarted(request: HttpIdempotencyRequest) {
        val signal = mutex.withLock {
            val scope = scope(request)
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
        val record = mutex.withLock { records[scope(request)] } ?: error("Owner was not started; values redacted.")
        val observed = record.waiterCount.first { count -> count == expected || count == RESET_WAITER_COUNT }
        check(observed != RESET_WAITER_COUNT) { "Scenario reset before waiter count was observed." }
    }

    suspend fun completeOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse) {
        val replayable = replayableOutcomeOrNull(outcome)
        if (replayable == null) {
            rejectUnsafeSnapshot(request)
            return
        }
        val selection = mutex.withLock {
            val record = records[scope(request)] ?: error("Owner was not started; values redacted.")
            check(record.state == RecordState.InFlight) { "Owner is not in flight; values redacted." }
            record.state = RecordState.Terminal
            record.response = replayable
            record.expiresAt = virtualNow.plus(config.retention)
            openGates.decrementAndGet()
            val waiters = record.waiters.values.map { waiter ->
                waiter to if (virtualNow >= waiter.deadline) timeoutResponse() else replayable.withReplayFlag(true)
            }
            removeAllWaiters(record)
            CompletionSelection(record.ownerCompletion to replayable.withReplayFlag(false), waiters)
        }
        selection.owner.first.complete(selection.owner.second)
        selection.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
    }

    suspend fun holdOwnerResponseDelivery(request: HttpIdempotencyRequest) {
        mutex.withLock {
            val scope = scope(request)
            check(scope !in records) { "Owner already started; values redacted." }
            responseDeliveryGates[scope] = CompletableDeferred()
            openGates.incrementAndGet()
        }
    }

    suspend fun releaseOwnerResponseDelivery(request: HttpIdempotencyRequest) {
        mutex.withLock { removeResponseDeliveryGate(scope(request)) }?.complete(Unit)
    }

    suspend fun abandonOwner(request: HttpIdempotencyRequest, outcome: HttpIdempotencyResponse) {
        val selection = mutex.withLock { abandonRecord(scope(request), outcome, completeOwner = true) }
        selection.owner?.let { (completion, response) -> completion.complete(response) }
        selection.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
    }

    suspend fun advanceTimeBy(duration: Duration) {
        require(!duration.isNegative) { "duration must not be negative." }
        val expired = mutex.withLock {
            virtualNow = virtualNow.plus(duration)
            records.values.flatMap { record ->
                if (record.state != RecordState.InFlight) return@flatMap emptyList()
                record.waiters.values.filter { waiter -> virtualNow >= waiter.deadline }.onEach { waiter ->
                    removeWaiter(record, waiter.id)
                }
            }
        }
        expired.forEach { waiter -> waiter.completion.complete(timeoutResponse()) }
        yield()
    }

    suspend fun resetScenario() {
        yield()
        val pending = mutex.withLock {
            PendingReset(
                owners = records.values.map { record -> record.ownerCompletion },
                waiters = records.values.flatMap { record -> record.waiters.values.map { waiter -> waiter.completion } },
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

    fun sideEffectCount(request: HttpIdempotencyRequest): Int =
        if (request.idempotencyKeys.size != 1 || request.authenticationProfile !in AUTHORIZED_PROFILES) 0
        else sideEffects[scope(request)]?.get() ?: 0

    fun quiescence(): HttpIdempotencyQuiescence = HttpIdempotencyQuiescence(
        activeWaiters = activeWaiters.get(),
        openGates = openGates.get(),
        activeChildTasks = 0,
    )

    private suspend fun execute(
        exchangeId: Long,
        tenant: String,
        request: HttpIdempotencyRequest,
    ): HttpIdempotencyResponse {
        validateIngress(request)?.let { return it }
        lookupCount++
        val scope = scope(tenant, request)
        val fingerprint = digest(checkNotNull(canonicalPayloadOrNull(request.requestBody)))
        val cancellation = checkNotNull(exchangeCancellations[exchangeId]) { "Exchange cancellation was not registered." }
        return try {
            when (val action = mutex.withLock {
                decideExchange(scope, fingerprint).also { selected ->
                    if (selected !is ExchangeAction.Immediate) exchangeActions[exchangeId] = selected
                }
            }) {
                is ExchangeAction.Owner -> awaitOwnerCompletion(action, cancellation.signal)
                is ExchangeAction.Waiter -> awaitWaiterCompletion(action, cancellation.signal)
                is ExchangeAction.Immediate -> action.response
            }
        } finally {
            withContext(NonCancellable) { mutex.withLock { exchangeActions.remove(exchangeId) } }
        }
    }

    private fun decideExchange(scope: String, fingerprint: String): ExchangeAction {
        val existing = records[scope]
        if (existing == null) return createOwner(scope, fingerprint)
        if (existing.state == RecordState.Terminal && virtualNow >= checkNotNull(existing.expiresAt)) {
            records.remove(scope)
            ownerSignals.remove(scope)
            return createOwner(scope, fingerprint)
        }
        if (existing.fingerprint != fingerprint) return ExchangeAction.Immediate(idempotencyResponse(409, "idempotency_key_reused"))
        return when (existing.state) {
            RecordState.InFlight -> registerWaiterOrReject(scope, existing)
            RecordState.Terminal -> ExchangeAction.Immediate(checkNotNull(existing.response).withReplayFlag(true))
            RecordState.Abandoned -> error("Abandoned records must not remain visible.")
        }
    }

    private fun createOwner(scope: String, fingerprint: String): ExchangeAction.Owner {
        val record = Record(fingerprint, responseDeliveryGates[scope])
        records[scope] = record
        sideEffects.computeIfAbsent(scope) { AtomicInteger() }.incrementAndGet()
        openGates.incrementAndGet()
        ownerSignals.getOrPut(scope) { CompletableDeferred() }.complete(Unit)
        return ExchangeAction.Owner(scope, record)
    }

    private fun registerWaiterOrReject(scope: String, record: Record): ExchangeAction {
        if (record.waiters.size >= config.maxWaitersPerKey) {
            return ExchangeAction.Immediate(idempotencyResponse(429, "idempotency_waiters_exceeded", config.overflowRetryAfter))
        }
        val waiter = Waiter(waiterSequence.incrementAndGet(), virtualNow.plus(config.waitTimeout))
        record.waiters[waiter.id] = waiter
        record.waiterCount.value = record.waiters.size
        activeWaiters.incrementAndGet()
        return ExchangeAction.Waiter(scope, record, waiter)
    }

    private suspend fun awaitOwnerCompletion(
        action: ExchangeAction.Owner,
        cancellation: CompletableDeferred<Unit>,
    ): HttpIdempotencyResponse {
        val response = select {
            action.record.ownerCompletion.onAwait { it }
            cancellation.onAwait {
                cancelOwnerExchange(action)
                idempotencyResponse(503, "temporarily_unavailable")
            }
        }
        action.record.responseDeliveryGate?.let { gate ->
            select {
                gate.onAwait { }
                cancellation.onAwait { releaseCancelledDelivery(action.scope) }
            }
        }
        return response
    }

    private suspend fun awaitWaiterCompletion(
        action: ExchangeAction.Waiter,
        cancellation: CompletableDeferred<Unit>,
    ): HttpIdempotencyResponse = try {
        select {
            action.waiter.completion.onAwait { it }
            cancellation.onAwait { idempotencyResponse(503, "temporarily_unavailable") }
        }
    } finally {
        withContext(NonCancellable) {
            mutex.withLock {
                val current = records[action.scope]
                if (current === action.record) removeWaiter(current, action.waiter.id)
            }
        }
    }

    private suspend fun cancelOwnerExchange(action: ExchangeAction.Owner) {
        val selection = withContext(NonCancellable) {
            mutex.withLock {
                val current = records[action.scope]
                if (current === action.record && current.state == RecordState.InFlight) {
                    abandonRecord(action.scope, idempotencyResponse(503, "temporarily_unavailable"), completeOwner = false)
                } else {
                    removeResponseDeliveryGate(action.scope)
                    AbandonSelection.EMPTY
                }
            }
        }
        selection.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
    }

    private suspend fun releaseCancelledDelivery(scope: String) {
        withContext(NonCancellable) {
            mutex.withLock { removeResponseDeliveryGate(scope) }
        }?.complete(Unit)
    }

    private fun abandonRecord(scope: String, outcome: HttpIdempotencyResponse, completeOwner: Boolean): AbandonSelection {
        val record = records[scope] ?: return AbandonSelection.EMPTY
        if (record.state != RecordState.InFlight) return AbandonSelection.EMPTY
        records.remove(scope)
        record.state = RecordState.Abandoned
        ownerSignals.remove(scope)
        openGates.decrementAndGet()
        removeResponseDeliveryGate(scope)
        val waiters = record.waiters.values.map { waiter -> waiter to outcome }
        removeAllWaiters(record)
        return AbandonSelection(if (completeOwner) record.ownerCompletion to outcome else null, waiters)
    }

    private suspend fun rejectUnsafeSnapshot(request: HttpIdempotencyRequest) {
        val selection = mutex.withLock {
            val scope = scope(request)
            val record = records.remove(scope) ?: error("Owner was not started; values redacted.")
            ownerSignals.remove(scope)
            openGates.decrementAndGet()
            removeResponseDeliveryGate(scope)
            sideEffects[scope]?.decrementAndGet()
            val response = idempotencyResponse(500, "idempotency_snapshot_rejected")
            val waiters = record.waiters.values.map { waiter -> waiter to response }
            removeAllWaiters(record)
            CompletionSelection(record.ownerCompletion to response, waiters)
        }
        selection.owner.first.complete(selection.owner.second)
        selection.waiters.forEach { (waiter, response) -> waiter.completion.complete(response) }
    }

    private fun validateIngress(request: HttpIdempotencyRequest): HttpIdempotencyResponse? {
        if (request.idempotencyKeys.size != 1) return idempotencyResponse(400, "invalid_idempotency_request")
        val key = request.idempotencyKeys.single()
        if (key.toByteArray().size > config.maxIdempotencyKeyBytes || key.isEmpty() ||
            key.any { character -> character.code !in 0x21..0x7e } || canonicalPayloadOrNull(request.requestBody) == null
        ) return idempotencyResponse(400, "invalid_idempotency_request")
        return null
    }

    private suspend fun ApplicationCall.receiveBoundedUtf8(maxBytes: Int): BoundedBodyRead {
        lastBodyReadBytes = 0
        request.contentLength()?.let { declared -> if (declared > maxBytes) return BoundedBodyRead.TooLarge }
        val bytes = receiveChannel().readRemaining(maxBytes.toLong() + 1L).readByteArray()
        lastBodyReadBytes = bytes.size
        if (bytes.size > maxBytes) return BoundedBodyRead.TooLarge
        return try {
            BoundedBodyRead.Value(
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString(),
            )
        } catch (_: CharacterCodingException) {
            BoundedBodyRead.Malformed
        }
    }

    private suspend fun ApplicationCall.respond(response: HttpIdempotencyResponse) {
        this.response.headers.append(TEST_RESPONSE_HEADERS, response.headers.keys.joinToString(","))
        response.headers.forEach { (name, values) -> values.forEach { value -> this.response.headers.append(name, value) } }
        response.problemCode?.let { code -> this.response.headers.append(PROBLEM_CODE, code) }
        val contentType = response.headers["content-type"]?.firstOrNull()?.let(ContentType::parse)
        respondText(response.body, contentType, HttpStatusCode.fromValue(response.statusCode))
    }

    private fun scope(request: HttpIdempotencyRequest): String = scope(
        when (request.authenticationProfile) {
            "tenant-a-principal" -> "tenant-a"
            "tenant-b-principal" -> "tenant-b"
            else -> error("Authentication must complete before idempotency lookup; values redacted.")
        },
        request,
    )

    private fun scope(tenant: String, request: HttpIdempotencyRequest): String =
        listOf(tenant, digest(request.operation), digest(request.resourceIdentity), digest(request.idempotencyKeys.single())).joinToString("|")

    private fun replayableOutcomeOrNull(outcome: HttpIdempotencyResponse): HttpIdempotencyResponse? {
        if (outcome.body.toByteArray().size > config.maxReplayBodyBytes) return null
        val connectionNominated = outcome.headers["connection"].orEmpty()
            .flatMap { value -> value.split(',') }.map { name -> name.trim().lowercase() }.filter(String::isNotEmpty).toSet()
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

    private fun isReplayHeaderDenied(name: String): Boolean =
        name in DENIED_REPLAY_HEADERS || name.contains("credential") || name.contains("secret") ||
                name.endsWith("-token") || name.endsWith("-api-key")

    private fun canonicalPayloadOrNull(body: String): String? = try {
        JsonCanonicalizer(body).canonicalize()
    } catch (_: InvalidJsonException) {
        null
    }

    private fun digest(value: String): String = HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()),
    )

    private fun timeoutResponse(): HttpIdempotencyResponse =
        idempotencyResponse(409, "idempotency_in_flight", config.inFlightRetryAfter)

    private fun removeAllWaiters(record: Record) {
        activeWaiters.addAndGet(-record.waiters.size)
        record.waiters.clear()
        record.waiterCount.value = 0
    }

    private fun removeWaiter(record: Record, id: Long) {
        if (record.waiters.remove(id) != null) {
            activeWaiters.decrementAndGet()
            record.waiterCount.value = record.waiters.size
        }
    }

    private fun removeResponseDeliveryGate(scope: String): CompletableDeferred<Unit>? =
        responseDeliveryGates.remove(scope)?.also { openGates.decrementAndGet() }

    private class Record(
        val fingerprint: String,
        val responseDeliveryGate: CompletableDeferred<Unit>?,
        var state: RecordState = RecordState.InFlight,
        var response: HttpIdempotencyResponse? = null,
        var expiresAt: Duration? = null,
        val ownerCompletion: CompletableDeferred<HttpIdempotencyResponse> = CompletableDeferred(),
        val waiters: MutableMap<Long, Waiter> = linkedMapOf(),
        val waiterCount: MutableStateFlow<Int> = MutableStateFlow(0),
    )

    private class Waiter(
        val id: Long,
        val deadline: Duration,
        val completion: CompletableDeferred<HttpIdempotencyResponse> = CompletableDeferred(),
    )

    private sealed interface RecordState {
        data object InFlight: RecordState
        data object Terminal: RecordState
        data object Abandoned: RecordState
    }

    private sealed interface ExchangeAction {
        class Owner(val scope: String, val record: Record): ExchangeAction
        class Waiter(val scope: String, val record: Record, val waiter: KtorFakeIdempotencyApplication.Waiter): ExchangeAction
        class Immediate(val response: HttpIdempotencyResponse): ExchangeAction
    }

    private class CompletionSelection(
        val owner: Pair<CompletableDeferred<HttpIdempotencyResponse>, HttpIdempotencyResponse>,
        val waiters: List<Pair<Waiter, HttpIdempotencyResponse>>,
    )

    private class AbandonSelection(
        val owner: Pair<CompletableDeferred<HttpIdempotencyResponse>, HttpIdempotencyResponse>?,
        val waiters: List<Pair<Waiter, HttpIdempotencyResponse>>,
    ) {
        companion object {
            val EMPTY = AbandonSelection(null, emptyList())
        }
    }

    private class CancelSelection(
        val abandon: AbandonSelection = AbandonSelection.EMPTY,
        val waiter: Waiter? = null,
        val deliveryGate: CompletableDeferred<Unit>? = null,
    ) {
        companion object {
            val EMPTY = CancelSelection()
        }
    }

    private class PendingReset(
        val owners: List<CompletableDeferred<HttpIdempotencyResponse>>,
        val waiters: List<CompletableDeferred<HttpIdempotencyResponse>>,
        val responseDeliveries: List<CompletableDeferred<Unit>>,
        val ownerSignals: List<CompletableDeferred<Unit>>,
    )
}

private sealed interface BoundedBodyRead {
    class Value(val value: String): BoundedBodyRead
    data object TooLarge: BoundedBodyRead
    data object Malformed: BoundedBodyRead
}

private class ExchangeCancellation(
    val signal: CompletableDeferred<Unit> = CompletableDeferred(),
)

private fun HttpIdempotencyResponse.withReplayFlag(replayed: Boolean): HttpIdempotencyResponse =
    copy(headers = headers + ("idempotency-replayed" to listOf(replayed.toString())))

private fun idempotencyResponse(status: Int, problemCode: String, retryAfter: Duration? = null): HttpIdempotencyResponse =
    HttpIdempotencyResponse(
        statusCode = status,
        body = "{\"code\":\"$problemCode\"}",
        headers = buildMap {
            put("content-type", listOf("application/problem+json"))
            retryAfter?.let { put("retry-after", listOf(it.seconds.toString())) }
        },
        problemCode = problemCode,
    )

private class JsonCanonicalizer(private val source: String) {
    private var index = 0
    private var depth = 0

    fun canonicalize(): String {
        skipWhitespace()
        val value = parseValue()
        skipWhitespace()
        if (index != source.length) invalidJson()
        return value
    }

    private fun parseValue(): String {
        skipWhitespace()
        return when (peek()) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> quote(parseString())
            't' -> parseLiteral("true")
            'f' -> parseLiteral("false")
            'n' -> parseLiteral("null")
            '-', in '0'..'9' -> parseNumber()
            else -> invalidJson()
        }
    }

    private fun parseObject(): String {
        enterContainer()
        expect('{')
        skipWhitespace()
        if (consume('}')) return "{}".also { leaveContainer() }
        val members = linkedMapOf<String, String>()
        while (true) {
            if (peek() != '"') invalidJson()
            val name = parseString()
            if (name in members) invalidJson()
            skipWhitespace()
            expect(':')
            members[name] = parseValue()
            skipWhitespace()
            if (consume('}')) break
            expect(',')
            skipWhitespace()
        }
        return members.entries.sortedBy { entry -> entry.key }
            .joinToString(prefix = "{", postfix = "}") { (name, value) -> "${quote(name)}:$value" }
            .also { leaveContainer() }
    }

    private fun parseArray(): String {
        enterContainer()
        expect('[')
        skipWhitespace()
        if (consume(']')) return "[]".also { leaveContainer() }
        val values = mutableListOf<String>()
        while (true) {
            values += parseValue()
            skipWhitespace()
            if (consume(']')) break
            expect(',')
        }
        return values.joinToString(prefix = "[", postfix = "]").also { leaveContainer() }
    }

    private fun parseString(): String {
        expect('"')
        val decoded = StringBuilder()
        while (index < source.length) {
            when (val character = source[index++]) {
                '"' -> return decoded.toString()
                '\\' -> decoded.append(parseEscape())
                else -> {
                    if (character.code < 0x20) invalidJson()
                    decoded.append(character)
                }
            }
        }
        invalidJson()
    }

    private fun parseEscape(): String {
        if (index >= source.length) invalidJson()
        return when (val escaped = source[index++]) {
            '"', '\\', '/' -> escaped.toString()
            'b' -> "\b"
            'f' -> "\u000c"
            'n' -> "\n"
            'r' -> "\r"
            't' -> "\t"
            'u' -> parseUnicodeEscape()
            else -> invalidJson()
        }
    }

    private fun parseUnicodeEscape(): String {
        val first = parseUnicodeCodeUnit()
        if (first.isLowSurrogate()) invalidJson()
        if (!first.isHighSurrogate()) return first.toString()
        if (index + 2 > source.length || source[index] != '\\' || source[index + 1] != 'u') invalidJson()
        index += 2
        val second = parseUnicodeCodeUnit()
        if (!second.isLowSurrogate()) invalidJson()
        return charArrayOf(first, second).concatToString()
    }

    private fun parseUnicodeCodeUnit(): Char {
        if (index + 4 > source.length) invalidJson()
        val digits = source.substring(index, index + 4)
        if (digits.any { digit -> digit.digitToIntOrNull(16) == null }) invalidJson()
        index += 4
        return digits.toInt(16).toChar()
    }

    private fun parseNumber(): String {
        val start = index
        consume('-')
        when (peek()) {
            '0' -> {
                index++
                if (peek() in '0'..'9') invalidJson()
            }
            in '1'..'9' -> while (peek() in '0'..'9') index++
            else -> invalidJson()
        }
        if (consume('.')) {
            if (peek() !in '0'..'9') invalidJson()
            while (peek() in '0'..'9') index++
        }
        if (peek() == 'e' || peek() == 'E') {
            index++
            if (peek() == '+' || peek() == '-') index++
            if (peek() !in '0'..'9') invalidJson()
            while (peek() in '0'..'9') index++
        }
        val number = try {
            BigDecimal(source.substring(start, index)).stripTrailingZeros()
        } catch (_: NumberFormatException) {
            invalidJson()
        }
        return if (number.compareTo(BigDecimal.ZERO) == 0) "0" else number.toString()
    }

    private fun parseLiteral(literal: String): String {
        if (!source.startsWith(literal, index)) invalidJson()
        index += literal.length
        return literal
    }

    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000c' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) {
                    append("\\u")
                    append(character.code.toString(16).padStart(4, '0'))
                } else append(character)
            }
        }
        append('"')
    }

    private fun expect(expected: Char) {
        if (!consume(expected)) invalidJson()
    }

    private fun consume(expected: Char): Boolean = if (peek() == expected) {
        index++
        true
    } else false

    private fun peek(): Char = source.getOrNull(index) ?: END_OF_INPUT

    private fun skipWhitespace() {
        while (peek() == ' ' || peek() == '\t' || peek() == '\n' || peek() == '\r') index++
    }

    private fun enterContainer() {
        depth++
        if (depth > MAX_JSON_DEPTH) invalidJson()
    }

    private fun leaveContainer() {
        depth--
    }

    private fun invalidJson(): Nothing = throw InvalidJsonException()
}

private class InvalidJsonException: IllegalArgumentException()

private val AUTHORIZED_PROFILES = setOf("tenant-a-principal", "tenant-b-principal")
private val DENIED_REPLAY_HEADERS = setOf(
    "authorization", "cookie", "set-cookie", "proxy-authorization", "www-authenticate", "connection",
    "authentication-info", "proxy-authenticate", "proxy-authentication-info", "keep-alive", "te", "trailer",
    "transfer-encoding", "upgrade", "x-api-key",
)
private const val TEST_AUTH_PROFILE = "X-Test-Auth-Profile"
private const val TEST_OPERATION = "X-Test-Operation"
private const val TEST_EXCHANGE_ID = "X-Test-Exchange-Id"
private const val TEST_RESPONSE_HEADERS = "X-Test-Response-Headers"
private const val IDEMPOTENCY_KEY = "Idempotency-Key"
private const val PROBLEM_CODE = "X-Problem-Code"
private const val CONTENT_TYPE = "content-type"
private const val RESET_WAITER_COUNT = -1
private const val END_OF_INPUT = '\u0000'
private const val MAX_JSON_DEPTH = 128
