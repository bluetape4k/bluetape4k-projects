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
class OpensearchServerTest {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `launch opensearch server`() {
            OpensearchServer().use { es ->
                es.start()
                es.isRunning.shouldBeTrue()

                val config = OpensearchServer.Launcher.getClientConfiguration(es)
                assertCreateRestClient(config)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch opensearch server with default port`() {
            OpensearchServer(useDefaultPort = true).use { es ->
                es.start()
                es.isRunning.shouldBeTrue()
                es.port shouldBeEqualTo OpensearchServer.HTTP_PORT

                val config = OpensearchServer.Launcher.getClientConfiguration(es)
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
