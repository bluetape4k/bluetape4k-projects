package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients
import io.bluetape4k.assertions.assertFailsWith

class OpenSearchServerTest: AbstractContainerTest() {

    companion object Companion: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch opensearch server`() {
            OpenSearchServer().use { es ->
                es.start()
                es.isRunning.shouldBeTrue()

                val config = OpenSearchServer.Launcher.Spring.getClientConfiguration(es)
                assertCreateRestClient(config)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch opensearch server with default port`() {
            OpenSearchServer(useDefaultPort = true).use { es ->
                es.start()
                es.isRunning.shouldBeTrue()
                es.port shouldBeEqualTo OpenSearchServer.HTTP_PORT

                val config = OpenSearchServer.Launcher.Spring.getClientConfiguration(es)
                assertCreateRestClient(config)
            }
        }
    }

    private fun assertCreateRestClient(config: ClientConfiguration) {
        val client = ElasticsearchClients.getRestClient(config)
        client.shouldNotBeNull()
        client.isRunning.shouldBeTrue()
        client.close()
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { OpenSearchServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { OpenSearchServer(tag = " ") }
    }
}
