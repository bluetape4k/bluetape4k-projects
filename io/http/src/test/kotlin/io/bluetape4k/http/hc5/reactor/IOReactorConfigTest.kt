package io.bluetape4k.http.hc5.reactor

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.util.TimeValue
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class IOReactorConfigTest {

    companion object : KLogging()

    @Test
    fun `ioReactorConfig DSL creates config with custom io thread count`() {
        val config = ioReactorConfig {
            setIoThreadCount(4)
        }

        config.shouldNotBeNull()
        config.ioThreadCount shouldBeEqualTo 4
    }

    @Test
    fun `ioReactorConfig DSL creates config with soTimeout`() {
        val timeout = Timeout.of(5000, TimeUnit.MILLISECONDS)
        val config = ioReactorConfig {
            setSoTimeout(timeout)
        }

        config.shouldNotBeNull()
        config.soTimeout shouldBeEqualTo timeout
    }

    @Test
    fun `ioReactorConfig DSL creates config with soKeepAlive`() {
        val config = ioReactorConfig {
            setSoKeepAlive(true)
        }

        config.shouldNotBeNull()
        config.isSoKeepAlive.shouldBeTrue()
    }

    @Test
    fun `ioReactorConfigOf with named parameters sets values correctly`() {
        val ioThreadCount = 2
        val soTimeout = Timeout.of(3000, TimeUnit.MILLISECONDS)
        val soLinger = TimeValue.ofSeconds(30)
        val soKeepAlive = true

        val config = ioReactorConfigOf(
            ioThreadCount = ioThreadCount,
            soTimeout = soTimeout,
            soLinger = soLinger,
            soKeepAlive = soKeepAlive,
        )

        config.shouldNotBeNull()
        config.ioThreadCount shouldBeEqualTo ioThreadCount
        config.soTimeout shouldBeEqualTo soTimeout
        config.soLinger shouldBeEqualTo soLinger
        config.isSoKeepAlive.shouldBeTrue()
    }

    @Test
    fun `ioReactorConfigOf uses defaults when not specified`() {
        val config = ioReactorConfigOf()

        config.shouldNotBeNull()
        config.ioThreadCount shouldBeEqualTo IOReactorConfig.DEFAULT.ioThreadCount
        config.soTimeout shouldBeEqualTo IOReactorConfig.DEFAULT.soTimeout
        config.soLinger shouldBeEqualTo IOReactorConfig.DEFAULT.soLinger
        config.isSoKeepAlive shouldBeEqualTo IOReactorConfig.DEFAULT.isSoKeepAlive
    }

    @Test
    fun `ioReactorConfigOf with additional DSL builder block`() {
        val config = ioReactorConfigOf(
            ioThreadCount = 2,
        ) {
            setSoKeepAlive(false)
            setTcpNoDelay(true)
        }

        config.shouldNotBeNull()
        config.ioThreadCount shouldBeEqualTo 2
        config.isSoKeepAlive.shouldBeFalse()
        config.isTcpNoDelay.shouldBeTrue()
    }
}
