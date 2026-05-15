package io.bluetape4k.kafka.streams

import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class StreamConfigTest {
    companion object: KLogging()

    @Test
    fun `streamsConfigDef는 not null이다`() {
        streamsConfigDef.shouldNotBeNull()
    }

    @Test
    fun `streamsConfigDef는 application_id 설정 키를 포함한다`() {
        val hasAppId = streamsConfigDef.configKeys().containsKey("application.id")
        hasAppId.shouldBeTrue()
    }

    @Test
    fun `streamsConfigDef는 bootstrap_servers 설정 키를 포함한다`() {
        val hasBootstrap = streamsConfigDef.configKeys().containsKey("bootstrap.servers")
        hasBootstrap.shouldBeTrue()
    }

    @Test
    fun `streamsConfigDef는 비어있지 않다`() {
        val keys = streamsConfigDef.configKeys()
        keys.isNotEmpty().shouldBeTrue()
    }
}
