package io.bluetape4k.grpc.testing.integration

import com.google.auth.oauth2.ComputeEngineCredentials
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.OAuth2Credentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.utils.Runtimex
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ClientStreamTracer
import io.grpc.ClientStreamTracer.StreamInfo
import io.grpc.Context
import io.grpc.Grpc
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.ServerCall
import io.grpc.ServerCall.Listener
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.ServerInterceptors
import io.grpc.ServerStreamTracer
import io.grpc.Status
import io.grpc.auth.MoreCallCredentials
import io.grpc.internal.testing.TestClientStreamTracer
import io.grpc.internal.testing.TestServerStreamTracer
import io.grpc.internal.testing.TestStreamTracer
import io.grpc.testing.TestUtils
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.assertj.core.util.VisibleForTesting
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.rules.DisableOnDebug
import org.junit.rules.TestRule
import java.io.IOException
import java.io.InputStream
import java.net.SocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertTrue

abstract class AbstractInteropTest {

    @get:Rule
    val globalTimeout: TestRule

    private val serverCallCapture = AtomicReference<ServerCall<*, *>>(null)
    private val clientCallCapture = AtomicReference<ClientCall<*, *>>(null)
    private val requestHeadersCapture = AtomicReference<Metadata?>()
    private val contextCapture = AtomicReference<Context>(null)
    private var testServiceExecutor: ScheduledExecutorService? = null
    private var server: Server? = null
    private val serverStreamTracers = LinkedBlockingQueue<ServerStreamTracerInfo>()

    private class ServerStreamTracerInfo(
        val fullMethodName: String,
        val tracer: InteropServerStreamTracer,
    ) {
        class InteropServerStreamTracer: TestServerStreamTracer() {
            @Volatile
            var contextCapture: Context? = null

            override fun filterContext(context: Context?): Context {
                contextCapture = context
                return super.filterContext(context)
            }
        }
    }

    private val serverStreamTracerFactory: ServerStreamTracer.Factory =
        object: ServerStreamTracer.Factory() {
            override fun newServerStreamTracer(fullMethodName: String, headers: io.grpc.Metadata): ServerStreamTracer {
                val tracer = ServerStreamTracerInfo.InteropServerStreamTracer()
                serverStreamTracers.add(ServerStreamTracerInfo(fullMethodName, tracer))
                return tracer
            }
        }

    private fun startServer() {
        val builder = serverBuilder
        if (builder == null) {
            server = null
            return
        }

        val executor = Executors.newScheduledThreadPool(2)
        testServiceExecutor = executor
        val allInterceptors = fastListOf<ServerInterceptor>(
            recordServerCallInterceptor(serverCallCapture),
            TestUtils.recordRequestHeadersInterceptor(requestHeadersCapture),
            recordContextInterceptor(contextCapture)
        ).apply {
            addAll(TestServiceImpl.interceptors)
        }

        builder
            .addService(ServerInterceptors.intercept(TestServiceImpl(executor), allInterceptors))
            .addStreamTracerFactory(serverStreamTracerFactory)

        server = try {
            builder.build().start()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun stopServer() {
        server?.shutdownNow()
        testServiceExecutor?.shutdown()
    }

    @get:VisibleForTesting
    val listenAddress: SocketAddress
        get() = server!!.listenSockets.first()

    protected lateinit var channel: ManagedChannel
    protected lateinit var stub: TestServiceGrpcKt.TestServiceCoroutineStub

    // to be deleted when subsclasses are ready to migrate
    @JvmField
    var blockingStub: TestServiceGrpc.TestServiceBlockingStub? = null

    // to be deleted when subsclasses are ready to migrate
    @JvmField
    var asyncStub: TestServiceGrpc.TestServiceStub? = null

    private val clientStreamTracers = LinkedBlockingQueue<TestClientStreamTracer>()
    private val clientStreamTracerFactory: ClientStreamTracer.Factory =
        object: ClientStreamTracer.Factory() {
            override fun newClientStreamTracer(info: StreamInfo, headers: Metadata): ClientStreamTracer {
                val tracer = TestClientStreamTracer()
                clientStreamTracers.add(tracer)
                return tracer
            }
        }
    private val tracerSetupInterceptor: ClientInterceptor = object: ClientInterceptor {
        override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: Channel,
        ): ClientCall<ReqT, RespT> {
            return next.newCall(method, callOptions.withStreamTracerFactory(clientStreamTracerFactory))
        }
    }

    protected abstract fun createChannel(): ManagedChannel

    protected val additionalInterceptors: Array<ClientInterceptor>? get() = null

    /**
     * Returns the server builder used to create server for each test run.  Return `null` if
     * it shouldn't start a server in the same process.
     */
    protected open val serverBuilder: ServerBuilder<*>? get() = null

    /**
     * Must be called by the subclass setup method if overridden.
     */
    @BeforeEach
    open fun setup() {
        startServer()
        channel = createChannel()
        stub = TestServiceGrpcKt.TestServiceCoroutineStub(channel).withInterceptors(tracerSetupInterceptor)
        blockingStub = TestServiceGrpc.newBlockingStub(channel).withInterceptors(tracerSetupInterceptor)
        asyncStub = TestServiceGrpc.newStub(channel).withInterceptors(tracerSetupInterceptor)

        val additionalInterceptors = additionalInterceptors
        if (additionalInterceptors != null) {
            stub.withInterceptors(*additionalInterceptors)
        }
        requestHeadersCapture.set(null)
    }

    @AfterEach
    fun cleanup() {
        channel.shutdownNow()
        try {
            channel.awaitTermination(1, TimeUnit.SECONDS)
        } catch (ie: InterruptedException) {
            log.debug(ie) { "Interrupted while waiting for channel termination" }
            Thread.currentThread().interrupt()
        }
        stopServer()
    }

    @Test
    fun emptyUnary() {
        runBlocking {
            stub.emptyCall(EMPTY) shouldBeEqualTo EMPTY
        }
    }

    @Test
    fun largeUnary() {
        assumeEnoughMemory()

        val request = Messages.SimpleRequest.newBuilder()
            .apply {
                responseSize = RESPONSE_SIZE
                payload = Messages.Payload.newBuilder().setBody(ByteString.copyFrom(ByteArray(REQUEST_SIZE))).build()
            }.build()

        val goldenResponse = Messages.SimpleResponse.newBuilder()
            .apply {
                payload = Messages.Payload.newBuilder().setBody(ByteString.copyFrom(ByteArray(RESPONSE_SIZE))).build()
            }.build()

        runBlocking {
            stub.unaryCall(request) shouldBeEqualTo goldenResponse
        }
    }

    /**
     * Verifies remote server address and local client address are available from ClientCall
     * Attributes via ClientInterceptor.
     */
    @Test
    fun `get server address and local address from client`() {
        obtainRemoteServerAddr().shouldNotBeNull()
        obtainLocalClientAddr().shouldNotBeNull()
    }

    /** Sends a large unary rpc with service account credentials.  */
    fun serviceAccountCreds(jsonKey: String, credentialsStream: InputStream?, authScope: String) {
        // cast to ServiceAccountCredentials to double-check the right type of object was created.
        var credentials: GoogleCredentials =
            GoogleCredentials.fromStream(credentialsStream) as ServiceAccountCredentials
        credentials = credentials.createScoped(authScope)
        val stub = this.stub.withCallCredentials(MoreCallCredentials.from(credentials))

        val request = Messages.SimpleRequest.newBuilder()
            .apply {
                fillUsername = true
                fillOauthScope = true
                responseSize = RESPONSE_SIZE
                payload = Messages.Payload.newBuilder().setBody(ByteString.copyFrom(ByteArray(REQUEST_SIZE))).build()
            }.build()

        val response = runBlocking { stub.unaryCall(request) }
        response.username.shouldNotBeBlank()
        jsonKey shouldContain response.username
        response.oauthScope.shouldNotBeBlank()
        authScope shouldContain response.oauthScope

        val goldenResponse = Messages.SimpleResponse.newBuilder()
            .apply {
                this.oauthScope = response.oauthScope
                this.username = response.username
                this.payload =
                    Messages.Payload.newBuilder().setBody(ByteString.copyFrom(ByteArray(RESPONSE_SIZE))).build()
            }.build()

        assertResponse(goldenResponse, response)
    }

    /** Sends a large unary rpc with compute engine credentials.  */
    fun computeEngineCreds(serviceAccount: String?, oauthScope: String) {
        val credentials = ComputeEngineCredentials.create()
        val stub = this.stub.withCallCredentials(MoreCallCredentials.from(credentials))
        val request = Messages.SimpleRequest.newBuilder()
            .apply {
                fillUsername = true
                fillOauthScope = true
                responseSize = RESPONSE_SIZE
                payload = Messages.Payload.newBuilder().setBody(ByteString.copyFrom(ByteArray(REQUEST_SIZE))).build()
            }.build()

        val response = runBlocking { stub.unaryCall(request) }
        response.username shouldBeEqualTo serviceAccount
        response.oauthScope.shouldNotBeBlank()
        oauthScope shouldContain response.oauthScope

        val goldenResponse = Messages.SimpleResponse.newBuilder()
            .apply {
                this.oauthScope = response.oauthScope
                this.username = response.username
                this.payload =
                    Messages.Payload.newBuilder().setBody(ByteString.copyFrom(ByteArray(RESPONSE_SIZE))).build()
            }.build()

        assertResponse(goldenResponse, response)
    }

    /** Sends an unary rpc with ComputeEngineChannelBuilder.  */
    fun computeEngineChannelCredentials(
        defaultServiceAccount: String,
        computeEngineStub: TestServiceGrpcKt.TestServiceCoroutineStub,
    ) = runBlocking<Unit> {
        val request = Messages.SimpleRequest.newBuilder()
            .apply {
                fillUsername = true
                responseSize = RESPONSE_SIZE
                payload = Messages.Payload.newBuilder().setBody(ByteString.copyFrom(ByteArray(REQUEST_SIZE))).build()
            }.build()
        val response = computeEngineStub.unaryCall(request)

        val goldenResponse = Messages.SimpleResponse.newBuilder()
            .apply {
                username = defaultServiceAccount
                payload = Messages.Payload.newBuilder()
                    .apply {
                        body = ByteString.copyFrom(ByteArray(RESPONSE_SIZE))
                    }.build()
            }.build()
        response shouldBeEqualTo goldenResponse
    }

    /** Test JWT-based auth.  */
    fun jwtTokenCreds(serviceAccountJson: InputStream?) {
        val request = Messages.SimpleRequest.newBuilder()
            .apply {
                responseSize = RESPONSE_SIZE
                payload = Messages.Payload.newBuilder().setBody(ByteString.copyFrom(ByteArray(REQUEST_SIZE))).build()
                fillUsername = true
            }.build()

        val credentials = GoogleCredentials.fromStream(serviceAccountJson) as ServiceAccountCredentials
        val response = runBlocking {
            stub.withCallCredentials(MoreCallCredentials.from(credentials)).unaryCall(request)
        }

        response.username shouldBeEqualTo credentials.clientEmail
        response.payload.body.size() shouldBeEqualTo 314159
    }

    /** Sends a unary rpc with raw oauth2 access token credentials.  */
    fun oauth2AuthToken(jsonKey: String, credentialsStream: InputStream, authScope: String) {
        var utilCredentials = GoogleCredentials.fromStream(credentialsStream)
        utilCredentials = utilCredentials.createScoped(authScope)
        val accessToken = utilCredentials.refreshAccessToken()
        val credentials = OAuth2Credentials.create(accessToken)
        val request = Messages.SimpleRequest.newBuilder()
            .apply {
                fillUsername = true
                fillOauthScope = true
            }.build()

        val response = runBlocking {
            stub.withCallCredentials(MoreCallCredentials.from(credentials)).unaryCall(request)
        }
        response.username.shouldNotBeBlank()
        jsonKey shouldContain response.username

        response.oauthScope.shouldNotBeBlank()
        authScope shouldContain response.oauthScope
    }

    /** Sends a unary rpc with "per rpc" raw oauth2 access token credentials.  */
    fun perRpcCreds(jsonKey: String, credentialsStream: InputStream, oauthScope: String) {
        // In gRpc Java, we don't have per Rpc credentials, user can use an intercepted stub only once
        // for that purpose.
        // So, this test is identical to oauth2_auth_token test.
        oauth2AuthToken(jsonKey, credentialsStream, oauthScope)
    }

    fun googleDefaultCredentials(
        defaultServiceAccount: String,
        googleDefaultStub: TestServiceGrpcKt.TestServiceCoroutineStub,
    ) {
        runBlocking {
            val request = Messages.SimpleRequest.newBuilder()
                .apply {
                    fillUsername = true
                    responseSize = RESPONSE_SIZE
                    payload =
                        Messages.Payload.newBuilder().setBody(ByteString.copyFrom(ByteArray(REQUEST_SIZE))).build()
                }.build()

            val response = googleDefaultStub.unaryCall(request)
            response.username shouldBeEqualTo defaultServiceAccount

            val goldenResponse = Messages.SimpleResponse.newBuilder()
                .apply {
                    username = defaultServiceAccount
                    payload =
                        Messages.Payload.newBuilder().setBody(ByteString.copyFrom(ByteArray(RESPONSE_SIZE))).build()
                }.build()

            assertResponse(goldenResponse, response)
        }
    }

    /** Helper for getting remote address from [io.grpc.ClientCall.getAttributes]  */
    private fun obtainRemoteServerAddr(): SocketAddress? = runBlocking {
        stub
            .withInterceptors(recordClientCallInterceptor(clientCallCapture))
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .unaryCall(Messages.SimpleRequest.getDefaultInstance())

        clientCallCapture.get()?.attributes?.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR)
    }


    /** Helper for getting local address from [io.grpc.ClientCall.getAttributes]  */
    private fun obtainLocalClientAddr(): SocketAddress? = runBlocking {
        stub
            .withInterceptors(recordClientCallInterceptor(clientCallCapture))
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .unaryCall(Messages.SimpleRequest.getDefaultInstance())

        clientCallCapture.get()?.attributes?.get(Grpc.TRANSPORT_ATTR_LOCAL_ADDR)
    }

    protected fun operationTimeoutMillis(): Int {
        return 5_000
    }

    private fun assertStatsTrace(
        method: String,
        status: Status.Code,
        requests: Collection<MessageLite>? = null,
        responses: Collection<MessageLite>? = null,
    ) {
        assertClientStatsTrace(method, status, requests, responses)
        assertServerStatsTrace(method, status, requests, responses)
    }

    private fun assertClientStatsTrace(
        method: String,
        code: Status.Code,
        requests: Collection<MessageLite>? = null,
        responses: Collection<MessageLite>? = null,
    ) {
        require(method.isNotBlank()) { "Method must not be null or blank" }

        // Tracer based stats
        val tracer = clientStreamTracers.poll()!!
        tracer.outboundHeaders.shouldBeTrue()

        // assertClientStatsTrace() is called right after application receives status,
        // but streamClosed() may be called slightly later than that.  So we need a timeout.
        try {
            tracer.await(5, TimeUnit.SECONDS).shouldBeTrue()
        } catch (e: InterruptedException) {
            throw AssertionError(e)
        }

        tracer.status.code shouldBeEqualTo code
        if (requests != null && responses != null) {
            checkTracers(tracer, requests, responses)
        }
    }

    // Failure is checked in the end by the passed flag.
    private fun assertServerStatsTrace(
        method: String,
        code: Status.Code,
        requests: Collection<MessageLite>? = null,
        responses: Collection<MessageLite>? = null,
    ) {
        if (server == null) {  // Server is not in the same process.  We can't check server-side stats.
            return
        }

        val tracerInfo = serverStreamTracers.poll()!!
        tracerInfo.fullMethodName shouldBeEqualTo method
        tracerInfo.tracer.contextCapture.shouldNotBeNull()

        // On the server, streamClosed() may be called after the client receives the final status.
        // So we use a timeout.
        try {
            tracerInfo.tracer.await(1, TimeUnit.SECONDS).shouldBeTrue()
        } catch (e: InterruptedException) {
            throw AssertionError(e)
        }
        tracerInfo.tracer.status.code shouldBeEqualTo code

        if (requests != null && responses != null) {
            checkTracers(tracerInfo.tracer, responses, requests)
        }

    }

    private fun checkTracers(
        tracer: TestStreamTracer,
        sentMessages: Collection<MessageLite>,
        receivedMessages: Collection<MessageLite>,
    ) {
        var uncompressedSentSize = 0L
        var seqNo = 0

        sentMessages.forEach { msg ->
            tracer.nextOutboundEvent() shouldBeEqualTo "outboundMessage($seqNo)"
            tracer.nextOutboundEvent()!!.matches(Regex("outboundMessageSent\\($seqNo, -?[0-9]+, -?[0-9]+\\)"))
            seqNo++
            uncompressedSentSize += msg.serializedSize.toLong()
        }

        tracer.nextOutboundEvent().shouldBeNull()
        var uncompressedReceivedSize = 0L
        seqNo = 0

        receivedMessages.forEach { msg ->
            tracer.nextInboundEvent() shouldBeEqualTo "inboundMessage($seqNo)"
            tracer.nextInboundEvent()!!.matches(Regex("inboundMessageRead\\($seqNo, -?[0-9]+, -?[0-9]+\\)"))
            seqNo++
            uncompressedReceivedSize += msg.serializedSize.toLong()
        }
        tracer.nextInboundEvent().shouldBeNull()
    }

    // Helper methods for responses containing Payload since proto equals does not ignore deprecated
    // fields (PayloadType).
    private fun assertResponses(
        expected: Collection<Messages.StreamingOutputCallResponse>,
        actual: Collection<Messages.StreamingOutputCallResponse>,
    ) {
        expected.size shouldBeEqualTo actual.size
        val expectedIter = expected.iterator()
        val actualIter = actual.iterator()
        while (expectedIter.hasNext()) {
            assertResponse(expectedIter.next(), actualIter.next())
        }
    }

    private fun assertResponse(
        expected: Messages.StreamingOutputCallResponse?,
        actual: Messages.StreamingOutputCallResponse?,
    ) {
        if (expected == null || actual == null) {
            expected shouldBeEqualTo actual
        } else {
            assertPayload(expected.payload, actual.payload)
        }
    }

    private fun assertResponse(expected: Messages.SimpleResponse, actual: Messages.SimpleResponse) {
        assertPayload(expected.payload, actual.payload)
        expected.username shouldBeEqualTo actual.username
        expected.oauthScope shouldBeEqualTo actual.oauthScope
    }

    private fun assertPayload(expected: Messages.Payload?, actual: Messages.Payload?) {
        // Compare non deprecated fields in Payload, to make this test forward compatible.
        if (expected == null || actual == null) {
            expected shouldBeEqualTo actual
        } else {
            expected.body shouldBeEqualTo actual.body
        }
    }

    companion object: KLogging() {
        const val MAX_MESSAGE_SIZE = 16 * 1024 * 1024

        const val REQUEST_SIZE = 271_828
        const val RESPONSE_SIZE = 314_159

        @JvmStatic
        protected val EMPTY: Emptys.Empty = Emptys.Empty.getDefaultInstance()

        private fun assumeEnoughMemory() {
            val availableMemory = Runtimex.availableMemory
            assertTrue("$availableMemory is not sufficient to run this test") {
                availableMemory >= 64 * 1024 * 1024
            }
        }

        /**
         * Capture the request attributes. Useful for testing ServerCalls.
         * [ServerCall.getAttributes]
         */
        private fun recordServerCallInterceptor(serverCallCapture: AtomicReference<ServerCall<*, *>>): ServerInterceptor {
            return object: ServerInterceptor {
                override fun <ReqT, RespT> interceptCall(
                    call: ServerCall<ReqT, RespT>,
                    headers: io.grpc.Metadata,
                    next: ServerCallHandler<ReqT, RespT>,
                ): Listener<ReqT> {
                    serverCallCapture.set(call)
                    return next.startCall(call, headers)
                }
            }
        }

        /**
         * Capture the request attributes. Useful for testing ClientCalls.
         * [ClientCall.getAttributes]
         */
        private fun recordClientCallInterceptor(clientCallCapture: AtomicReference<ClientCall<*, *>>): ClientInterceptor {
            return object: ClientInterceptor {
                override fun <ReqT, RespT> interceptCall(
                    method: MethodDescriptor<ReqT, RespT>,
                    callOptions: CallOptions?,
                    next: Channel,
                ): ClientCall<ReqT, RespT> {
                    return next.newCall(method, callOptions).also { clientCallCapture.set(it) }
                }
            }
        }

        private fun recordContextInterceptor(contextCapture: AtomicReference<Context>): ServerInterceptor {
            return object: ServerInterceptor {
                override fun <ReqT, RespT> interceptCall(
                    call: ServerCall<ReqT, RespT>,
                    headers: io.grpc.Metadata,
                    next: ServerCallHandler<ReqT, RespT>,
                ): Listener<ReqT> {
                    contextCapture.set(Context.current())
                    return next.startCall(call, headers)
                }
            }
        }
    }

    /**
     * Constructor for tests.
     */
    init {
        var timeout: TestRule = org.junit.rules.Timeout.seconds(60)
        try {
            timeout = DisableOnDebug(timeout)
        } catch (e: Throwable) {
            log.warn(e) { "Debugging not disabled." }
        }
        globalTimeout = timeout
    }
}
