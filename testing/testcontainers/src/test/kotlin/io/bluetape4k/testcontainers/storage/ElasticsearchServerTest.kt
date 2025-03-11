package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients

@Disabled("Use OpensearchServer or ElasticsearchOssServer instead")
@Execution(ExecutionMode.SAME_THREAD)
class ElasticsearchServerTest {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {

        @Test
        fun `launch elasticsearch`() {
            ElasticsearchServer().use { es ->
                es.start()
                es.isRunning.shouldBeTrue()
            }
        }

        @Test
        fun `launch elastic search with ssl`() {
            ElasticsearchServer(password = "wow-world").use { es ->
                es.start()
                es.isRunning.shouldBeTrue()

                val config = ElasticsearchServer.Launcher.getClientConfiguration(es)
                val client = ElasticsearchClients.getRestClient(config)
                client.isRunning.shouldBeTrue()
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `launch elasticsearch with default port`() {
            ElasticsearchServer(useDefaultPort = true).use { es ->
                es.start()
                es.isRunning.shouldBeTrue()
                es.port shouldBeEqualTo ElasticsearchServer.PORT
            }
        }
    }
}
