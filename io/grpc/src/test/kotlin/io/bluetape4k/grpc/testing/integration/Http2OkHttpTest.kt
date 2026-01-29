package io.bluetape4k.grpc.testing.integration

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.logging.KLogging
import io.grpc.ManagedChannel
import io.grpc.ServerBuilder
import io.grpc.internal.testing.StreamRecorder
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyServerBuilder
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.okhttp.internal.Platform
import io.grpc.okhttp.internal.TlsVersion
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import io.netty.handler.ssl.SupportedCipherSuiteFilter
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.InetSocketAddress
import javax.net.ssl.SSLContext

@Disabled("보안관련 설정 후에 테스트해야 합니다")
class Http2OkHttpTest: AbstractInteropTest() {

    companion object: KLogging() {
        const val BAD_HOSTNAME = "I.am.a.bad.hostname"
    }

    override val serverBuilder: ServerBuilder<*>?
        get() = try {
            var sslProvider = SslContext.defaultServerProvider()
            if (sslProvider == SslProvider.OPENSSL && !SslProvider.isAlpnSupported(sslProvider)) {
                // OkHttp only supports Jetty ALPN on OpenJDK. So if OpenSSL doesn't support ALPN, then we
                // are forced to use Jetty ALPN for Netty instead of OpenSSL.
                sslProvider = SslProvider.JDK
            }
            val contextBuilder = SslContextBuilder
                .forServer(TestUtils.loadCert("server1.pem"), TestUtils.loadCert("server1.key"))

            GrpcSslContexts.configure(contextBuilder, sslProvider)
            contextBuilder.ciphers(
                SSLContext.getDefault().defaultSSLParameters.cipherSuites.toFastList(),
                SupportedCipherSuiteFilter.INSTANCE
            )

            NettyServerBuilder.forPort(0)
                .flowControlWindow(65 * 1024)
                .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                .sslContext(contextBuilder.build())
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }

    override fun createChannel(): ManagedChannel {
        return createChannelBuilder().build()
    }

    private fun createChannelBuilder(): OkHttpChannelBuilder {
        val port = (listenAddress as InetSocketAddress).port
        val builder = OkHttpChannelBuilder.forAddress("localhost", port)
            .maxInboundMessageSize(MAX_MESSAGE_SIZE)
            .tlsConnectionSpec(
                arrayOf(TlsVersion.TLS_1_3.name, TlsVersion.TLS_1_2.name),
                SSLContext.getDefault().defaultSSLParameters.cipherSuites
            )
            .overrideAuthority(Util.authorityFromHostAndPort(TestUtils.TEST_SERVER_HOST, port))

        try {
            builder.sslSocketFactory(
                TestUtils.newSslSocketFactoryForCa(
                    Platform.get().provider,
                    TestUtils.loadCert("ca.pem")
                )
            )
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return builder
    }

    @BeforeAll
    fun loadConscrypt() {
        TestUtils.installConscryptIfAvailable()
    }

    @Test
    fun `received Data for finished stream`() {
        val responseParameters = Messages.ResponseParameters.newBuilder().setSize(1)
        val requestBuilder = Messages.StreamingOutputCallRequest.newBuilder()

        repeat(1000) {
            requestBuilder.addResponseParameters(responseParameters)
        }

        val recorder = StreamRecorder.create<Messages.StreamingOutputCallResponse>()

        val requestStream = asyncStub!!.fullDuplexCall(recorder)

        val request = requestBuilder.build()
        requestStream.onNext(request)
        recorder.firstValue().get()
        requestStream.onError(Exception("failed"))

        recorder.awaitCompletion()

        blockingStub!!.emptyCall(EMPTY) shouldBeEqualTo EMPTY
    }
}
