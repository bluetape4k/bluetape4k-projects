package io.bluetape4k.http.hc5.http2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.core5.http2.config.H2Config
import org.junit.jupiter.api.Test

class H2ConfigTest {

    companion object : KLogging()

    @Test
    fun `h2ConfigOf returns default H2Config`() {
        val config = h2ConfigOf()

        config.shouldNotBeNull()
        config shouldBeEqualTo H2Config.DEFAULT
    }

    @Test
    fun `h2Config DSL creates config with custom settings`() {
        val config = h2Config {
            setPushEnabled(true)
            setHeaderTableSize(4096)
            setInitialWindowSize(65535)
        }

        config.shouldNotBeNull()
        config.isPushEnabled.shouldBeTrue()
        config.headerTableSize shouldBeEqualTo 4096
        config.initialWindowSize shouldBeEqualTo 65535
    }

    @Test
    fun `h2Config from source creates copy with modifications`() {
        val source = h2Config {
            setPushEnabled(false)
            setHeaderTableSize(8192)
        }

        val modified = h2Config(source) {
            setPushEnabled(true)
        }

        modified.shouldNotBeNull()
        modified.isPushEnabled.shouldBeTrue()
        modified.headerTableSize shouldBeEqualTo 8192
    }

    @Test
    fun `h2Config with named parameters sets values correctly`() {
        val config = h2Config(
            pushEnabled = false,
            headerTableSize = 2048,
            initialWindowSize = 32768,
            compressionEnabled = true,
        )

        config.shouldNotBeNull()
        config.isPushEnabled.shouldBeFalse()
        config.headerTableSize shouldBeEqualTo 2048
        config.initialWindowSize shouldBeEqualTo 32768
        config.isCompressionEnabled.shouldBeTrue()
    }

    @Test
    fun `h2Config with named parameters uses defaults when not specified`() {
        val config = h2Config()

        config.shouldNotBeNull()
        config.isPushEnabled shouldBeEqualTo H2Config.DEFAULT.isPushEnabled
        config.headerTableSize shouldBeEqualTo H2Config.DEFAULT.headerTableSize
        config.initialWindowSize shouldBeEqualTo H2Config.DEFAULT.initialWindowSize
        config.isCompressionEnabled shouldBeEqualTo H2Config.DEFAULT.isCompressionEnabled
    }

    @Test
    fun `h2Config with named parameters and additional DSL block`() {
        val config = h2Config(
            pushEnabled = true,
            headerTableSize = 4096,
        ) {
            setCompressionEnabled(true)
        }

        config.shouldNotBeNull()
        config.isPushEnabled.shouldBeTrue()
        config.headerTableSize shouldBeEqualTo 4096
        config.isCompressionEnabled.shouldBeTrue()
    }
}
