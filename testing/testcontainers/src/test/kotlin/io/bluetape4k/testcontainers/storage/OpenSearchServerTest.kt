package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients

@Execution(ExecutionMode.SAME_THREAD)
class OpenSearchServerTest {

    companion object Companion: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch opensearch server`() {
            OpenSearchServer().use { es ->
                es.start()
                es.isRunning.shouldBeTrue()

                val config = OpenSearchServer.Launcher.getClientConfiguration(es)
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

                val config = OpenSearchServer.Launcher.getClientConfiguration(es)
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
}
