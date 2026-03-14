package io.bluetape4k.grpc.interceptor

import io.bluetape4k.grpc.examples.helloworld.GreeterGrpcKt
import io.bluetape4k.grpc.examples.helloworld.GreeterService
import io.bluetape4k.grpc.examples.helloworld.HelloRequest
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.grpc.Metadata
import io.grpc.ServerInterceptors
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

/**
 * [echoRequestHeadersInterceptor], [echoRequestMetadataInHeaders], [echoRequestMetadataInTrailers] 인터셉터 테스트
 */
class ServerInterceptorSupportTest {
    companion object : KLogging() {
        private val X_ID_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-id", Metadata.ASCII_STRING_MARSHALLER)
        private val X_TOKEN_KEY: Metadata.Key<String> =
            Metadata.Key.of("x-token", Metadata.ASCII_STRING_MARSHALLER)
    }

    private lateinit var serverName: String
    private lateinit var channel: io.grpc.ManagedChannel
    private lateinit var stub: GreeterGrpcKt.GreeterCoroutineStub

    @BeforeEach
    fun setup() {
        serverName = InProcessServerBuilder.generateName()
    }

    @AfterEach
    fun cleanup() {
        if (::channel.isInitialized && !channel.isShutdown) {
            channel.shutdown()
            channel.awaitTermination(3, TimeUnit.SECONDS)
        }
    }

    private fun buildServerWithInterceptor(interceptor: io.grpc.ServerInterceptor) {
        InProcessServerBuilder
            .forName(serverName)
            .addService(ServerInterceptors.intercept(GreeterService(), interceptor))
            .build()
            .start()

        channel =
            InProcessChannelBuilder
                .forName(serverName)
                .usePlaintext()
                .build()
        stub = GreeterGrpcKt.GreeterCoroutineStub(channel)
    }

    // ── echoRequestHeadersInterceptor ──────────────────────────────────────

    @Test
    fun `echoRequestHeadersInterceptor - 요청 헤더 키가 응답에 전파되어야 한다`() =
        runSuspendTest {
            buildServerWithInterceptor(echoRequestHeadersInterceptor(X_ID_KEY))

            val requestMetadata = Metadata()
            requestMetadata.put(X_ID_KEY, "test-id-123")

            val request = HelloRequest.newBuilder().setName("World").build()
            val response = stub.sayHello(request)
            response.shouldNotBeNull()
            response.message shouldBeEqualTo "Hello World"
        }

    @Test
    fun `echoRequestHeadersInterceptor - 키가 없으면 전파하지 않아야 한다`() =
        runSuspendTest {
            buildServerWithInterceptor(echoRequestHeadersInterceptor(X_ID_KEY))

            val request = HelloRequest.newBuilder().setName("NoHeader").build()
            val response = stub.sayHello(request)
            response.shouldNotBeNull()
            response.message shouldBeEqualTo "Hello NoHeader"
        }

    @Test
    fun `echoRequestHeadersInterceptor - 여러 키를 등록할 수 있어야 한다`() =
        runSuspendTest {
            buildServerWithInterceptor(echoRequestHeadersInterceptor(X_ID_KEY, X_TOKEN_KEY))

            val request = HelloRequest.newBuilder().setName("MultiKey").build()
            val response = stub.sayHello(request)
            response.shouldNotBeNull()
            response.message shouldBeEqualTo "Hello MultiKey"
        }

    @Test
    fun `echoRequestHeadersInterceptor - 키 없이 생성해도 정상 동작해야 한다`() =
        runSuspendTest {
            buildServerWithInterceptor(echoRequestHeadersInterceptor())

            val request = HelloRequest.newBuilder().setName("Empty").build()
            val response = stub.sayHello(request)
            response.shouldNotBeNull()
            response.message shouldBeEqualTo "Hello Empty"
        }

    // ── echoRequestMetadataInHeaders ───────────────────────────────────────

    @Test
    fun `echoRequestMetadataInHeaders - 헤더 인터셉터가 정상 동작해야 한다`() =
        runSuspendTest {
            buildServerWithInterceptor(echoRequestMetadataInHeaders(X_ID_KEY))

            val request = HelloRequest.newBuilder().setName("HeaderOnly").build()
            val response = stub.sayHello(request)
            response.shouldNotBeNull()
            response.message shouldBeEqualTo "Hello HeaderOnly"
        }

    @Test
    fun `echoRequestMetadataInHeaders - 여러 키를 등록할 수 있어야 한다`() =
        runSuspendTest {
            buildServerWithInterceptor(echoRequestMetadataInHeaders(X_ID_KEY, X_TOKEN_KEY))

            val request = HelloRequest.newBuilder().setName("MultiKeyHeader").build()
            val response = stub.sayHello(request)
            response.shouldNotBeNull()
            response.message shouldBeEqualTo "Hello MultiKeyHeader"
        }

    // ── echoRequestMetadataInTrailers ──────────────────────────────────────

    @Test
    fun `echoRequestMetadataInTrailers - 트레일러 인터셉터가 정상 동작해야 한다`() =
        runSuspendTest {
            buildServerWithInterceptor(echoRequestMetadataInTrailers(X_ID_KEY))

            val request = HelloRequest.newBuilder().setName("TrailerOnly").build()
            val response = stub.sayHello(request)
            response.shouldNotBeNull()
            response.message shouldBeEqualTo "Hello TrailerOnly"
        }

    @Test
    fun `echoRequestMetadataInTrailers - 여러 키를 등록할 수 있어야 한다`() =
        runSuspendTest {
            buildServerWithInterceptor(echoRequestMetadataInTrailers(X_ID_KEY, X_TOKEN_KEY))

            val request = HelloRequest.newBuilder().setName("MultiKeyTrailer").build()
            val response = stub.sayHello(request)
            response.shouldNotBeNull()
            response.message shouldBeEqualTo "Hello MultiKeyTrailer"
        }
}
