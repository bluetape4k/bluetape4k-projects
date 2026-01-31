package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RedpandaServerTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val TEST_TOPIC_NAME = "redpanda-test-topic-1"
        private const val TEST_TOPIC_NAME_CORUTINE = "redpanda-test-topic-coroutines-1"
    }

    @Nested
    inner class UseDockerPort {
        @Test
        fun `Launch RedpandarServer`() {
            RedpandaServer().use { redpanda ->
                redpanda.start()
                redpanda.isRunning.shouldBeTrue()

                log.debug { "Redpanda bootstrapServers=${redpanda.bootstrapServers}" }
                redpanda.bootstrapServers.shouldNotBeEmpty()
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `Launch RedpandarServer`() {
            RedpandaServer(useDefaultPort = true).use { redpanda ->
                redpanda.start()
                redpanda.isRunning.shouldBeTrue()

                log.debug { "Redpanda bootstrapServers=${redpanda.bootstrapServers}" }
                redpanda.bootstrapServers.shouldNotBeEmpty()
                redpanda.port shouldBeEqualTo RedpandaServer.PORT
            }
        }
    }
}
