package io.bluetape4k.testcontainers.mq

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

@Disabled("사용 빈도가 낮고, 파일 사이즈가 크다")
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

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { RedpandaServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { RedpandaServer(tag = " ") }
    }
}
