package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.core5.http.config.Http1Config
import org.apache.hc.core5.http.ssl.TLS
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test

class ConfigBuilderTest {

    companion object : KLogging()

    // ---- ConnectionConfig ----

    @Test
    fun `connectionConfig DSL - 기본 생성 검증`() {
        val config = connectionConfig {
            setConnectTimeout(Timeout.ofSeconds(10))
            setSocketTimeout(Timeout.ofSeconds(30))
        }

        config.shouldNotBeNull()
    }

    @Test
    fun `connectionConfig DSL - connectTimeout 설정 검증`() {
        val timeout = Timeout.ofSeconds(15)
        val config = connectionConfig {
            setConnectTimeout(timeout)
        }

        config.shouldNotBeNull()
        config.connectTimeout shouldBeEqualTo timeout
    }

    @Test
    fun `connectionConfigOf - 파라미터 기반 생성 검증`() {
        val connectTimeout = Timeout.ofSeconds(5)
        val socketTimeout = Timeout.ofSeconds(20)
        val config = connectionConfigOf(
            connectTimeout = connectTimeout,
            socketTimeout = socketTimeout,
        )

        config.shouldNotBeNull()
        config.connectTimeout shouldBeEqualTo connectTimeout
        config.socketTimeout shouldBeEqualTo socketTimeout
    }

    @Test
    fun `connectionConfigOf - validateAfterInactivity 설정 검증`() {
        val validateAfterInactivity = TimeValue.ofSeconds(60)
        val config = connectionConfigOf(
            validateAfterInactivity = validateAfterInactivity,
        )

        config.shouldNotBeNull()
        config.validateAfterInactivity shouldBeEqualTo validateAfterInactivity
    }

    @Test
    fun `connectionConfigOf - timeToLive 설정 검증`() {
        val timeToLive = TimeValue.ofMinutes(5)
        val config = connectionConfigOf(
            timeToLive = timeToLive,
        )

        config.shouldNotBeNull()
        config.timeToLive shouldBeEqualTo timeToLive
    }

    // ---- RequestConfig ----

    @Test
    fun `requestConfig DSL - 기본 생성 검증`() {
        val config = requestConfig {}

        config.shouldNotBeNull()
    }

    @Test
    fun `requestConfigOf - 기본 인스턴스 생성 검증`() {
        val config = requestConfigOf()

        config.shouldNotBeNull()
    }

    @Test
    fun `requestConfig DSL - connectionRequestTimeout 설정 검증`() {
        val timeout = Timeout.ofSeconds(10)
        val config = requestConfig {
            setConnectionRequestTimeout(timeout)
        }

        config.shouldNotBeNull()
        config.connectionRequestTimeout shouldBeEqualTo timeout
    }

    // ---- SocketConfig ----

    @Test
    fun `socketConfig DSL - 기본 생성 검증`() {
        val config = socketConfig {
            setSoTimeout(Timeout.ofSeconds(30))
        }

        config.shouldNotBeNull()
    }

    @Test
    fun `socketConfigOf - 기본값 생성 검증`() {
        val config = socketConfigOf()

        config.shouldNotBeNull()
    }

    @Test
    fun `socketConfigOf - soTimeout 파라미터 검증`() {
        val soTimeout = Timeout.ofSeconds(45)
        val config = socketConfigOf(soTimeout = soTimeout)

        config.shouldNotBeNull()
        config.soTimeout shouldBeEqualTo soTimeout
    }

    // ---- TlsConfig ----

    @Test
    fun `tlsConfig DSL - 기본 생성 검증`() {
        val config = tlsConfig {
            setSupportedProtocols(TLS.V_1_2, TLS.V_1_3)
        }

        config.shouldNotBeNull()
    }

    @Test
    fun `tlsConfigOf - 기본 프로토콜 생성 검증`() {
        val config = tlsConfigOf()

        config.shouldNotBeNull()
    }

    @Test
    fun `tlsConfigOf - handshakeTimeout 설정 검증`() {
        val handshakeTimeout = Timeout.ofSeconds(10)
        val config = tlsConfigOf(handshakeTimeout = handshakeTimeout)

        config.shouldNotBeNull()
        config.handshakeTimeout shouldBeEqualTo handshakeTimeout
    }

    @Test
    fun `tlsConfigOf - versionPolicy 설정 검증`() {
        val config = tlsConfigOf(versionPolicy = HttpVersionPolicy.NEGOTIATE)

        config.shouldNotBeNull()
        config.httpVersionPolicy shouldBeEqualTo HttpVersionPolicy.NEGOTIATE
    }

    // ---- Http1Config ----

    @Test
    fun `http1ConfigOf - 기본 인스턴스 반환 검증`() {
        val config = http1ConfigOf()

        config.shouldNotBeNull()
        config shouldBeEqualTo Http1Config.DEFAULT
    }

    @Test
    fun `http1Config DSL - bufferSize 설정 검증`() {
        val config = http1Config {
            setBufferSize(8192)
        }

        config.shouldNotBeNull()
        config.bufferSize shouldBeEqualTo 8192
    }

    @Test
    fun `http1Config(source) - 소스 복사 후 수정 검증`() {
        val source = http1ConfigOf()
        val config = http1Config(source) {
            setBufferSize(4096)
        }

        config.shouldNotBeNull()
        config.bufferSize shouldBeEqualTo 4096
    }

    // ---- CharCodingConfig ----

    @Test
    fun `charCodingConfigOf - 기본 인스턴스 반환 검증`() {
        val config = charCodingConfigOf()

        config.shouldNotBeNull()
    }

    @Test
    fun `charCodingConfig DSL - charset 설정 검증`() {
        val config = charCodingConfig {
            setCharset(Charsets.UTF_8)
        }

        config.shouldNotBeNull()
        config.charset shouldBeEqualTo Charsets.UTF_8
    }

    @Test
    fun `charCodingConfig(source) - 소스 복사 후 charset 변경 검증`() {
        val source = charCodingConfig {
            setCharset(Charsets.UTF_8)
        }
        val config = charCodingConfig(source) {
            setCharset(Charsets.ISO_8859_1)
        }

        config.shouldNotBeNull()
        config.charset shouldBeEqualTo Charsets.ISO_8859_1
    }
}
