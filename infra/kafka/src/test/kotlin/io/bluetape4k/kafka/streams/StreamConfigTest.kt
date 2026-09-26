package io.bluetape4k.kafka.streams

import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class StreamConfigTest {

    companion object: KLoggingChannel()

    @Test
    fun `streamsConfigDef 조회`() {
        val configDef = streamsConfigDef

        configDef.configKeys().forEach { (key, configKey) ->
            log.debug { "key: $key, config key name: ${configKey.name}, defaultValue: ${configKey.defaultValue}" }
        }
        configDef.configKeys().containsKey("application.id").shouldBeTrue()
    }
}
