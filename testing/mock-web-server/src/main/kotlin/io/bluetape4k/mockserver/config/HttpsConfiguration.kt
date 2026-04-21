package io.bluetape4k.mockserver.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.apache.catalina.connector.Connector
import org.apache.coyote.http11.AbstractHttp11Protocol
import org.apache.tomcat.util.net.SSLHostConfig
import org.apache.tomcat.util.net.SSLHostConfigCertificate
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

/**
 * Tomcat에 HTTPS 추가 커넥터를 등록하는 설정.
 *
 * 기존 HTTP 포트(8888)는 그대로 유지하고,
 * [httpsPort](기본값 8443)에 SSL이 활성화된 별도 커넥터를 추가한다.
 * 인증서는 `src/main/resources/certs/localhost.p12`에 번들되어 있다.
 */
@Configuration
class HttpsConfiguration {

    companion object : KLogging()

    /**
     * Tomcat에 HTTPS 커넥터를 추가하는 [WebServerFactoryCustomizer] 빈.
     *
     * @param httpsPort HTTPS 포트 번호 (기본값 8443)
     * @param keyStorePassword PKCS12 keystore 비밀번호
     */
    @Bean
    fun httpsConnectorCustomizer(
        @Value("\${bluetape4k.https.port:8443}") httpsPort: Int,
        @Value("\${bluetape4k.https.key-store-password:changeit}") keyStorePassword: String,
    ): WebServerFactoryCustomizer<TomcatServletWebServerFactory> = WebServerFactoryCustomizer { factory ->
        log.info { "HTTPS 커넥터 추가: port=$httpsPort" }
        factory.addAdditionalConnectors(createHttpsConnector(httpsPort, keyStorePassword))
    }

    private fun createHttpsConnector(port: Int, password: String): Connector {
        val p12Stream = HttpsConfiguration::class.java.getResourceAsStream("/certs/localhost.p12")
            ?: error("certs/localhost.p12 를 classpath에서 찾을 수 없습니다")

        val tmpFile = File.createTempFile("bluetape4k-https-", ".p12").also { it.deleteOnExit() }
        tmpFile.outputStream().use { out -> p12Stream.copyTo(out) }

        val connector = Connector("org.apache.coyote.http11.Http11NioProtocol")
        connector.scheme = "https"
        connector.secure = true
        connector.port = port

        val http11 = connector.protocolHandler as AbstractHttp11Protocol<*>
        http11.setSSLEnabled(true)

        val sslHostConfig = SSLHostConfig()
        val certificate = SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED)
        certificate.setCertificateKeystoreFile(tmpFile.absolutePath)
        certificate.setCertificateKeystorePassword(password)
        certificate.setCertificateKeystoreType("PKCS12")
        sslHostConfig.addCertificate(certificate)

        http11.addSslHostConfig(sslHostConfig)

        return connector
    }
}
