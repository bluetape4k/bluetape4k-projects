package io.bluetape4k.mockwebflux.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.netty.handler.ssl.SslContextBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.stereotype.Component
import reactor.netty.DisposableServer
import reactor.netty.http.server.HttpServer
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory

/**
 * Reactor Netty를 사용해 HTTPS 보조 서버를 [SmartLifecycle]로 구동하는 컴포넌트.
 *
 * 기존 HTTP 포트(9999)는 그대로 유지하고,
 * [httpsPort](기본값 9443)에 SSL이 활성화된 별도 Netty 서버를 추가한다.
 * 인증서는 `src/main/resources/certs/localhost.p12`에 번들되어 있다.
 */
@Component
class HttpsServerLifecycle(
    private val httpHandler: HttpHandler,
    @Value("\${bluetape4k.https.port:9443}") private val httpsPort: Int,
    @Value("\${bluetape4k.https.key-store-password:changeit}") private val keyStorePassword: String,
) : SmartLifecycle {

    companion object : KLogging()

    private var disposableServer: DisposableServer? = null

    override fun start() {
        val sslContext = buildSslContext()
        disposableServer = HttpServer.create()
            .port(httpsPort)
            .secure { spec -> spec.sslContext(sslContext) }
            .handle(ReactorHttpHandlerAdapter(httpHandler))
            .bindNow()
        log.info { "HTTPS 보조 서버 시작: port=$httpsPort" }
    }

    override fun stop() {
        disposableServer?.dispose()
        disposableServer = null
        log.info { "HTTPS 보조 서버 종료: port=$httpsPort" }
    }

    override fun isRunning(): Boolean = disposableServer?.isDisposed == false

    private fun buildSslContext(): io.netty.handler.ssl.SslContext {
        val p12Stream = HttpsServerLifecycle::class.java.getResourceAsStream("/certs/localhost.p12")
            ?: error("certs/localhost.p12 를 classpath에서 찾을 수 없습니다")

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(p12Stream, keyStorePassword.toCharArray())

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, keyStorePassword.toCharArray())

        return SslContextBuilder.forServer(kmf).build()
    }
}
