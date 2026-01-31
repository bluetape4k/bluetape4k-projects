package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients

class ElasticsearchOssServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `Elasticsearch OSS 서버 실행`() {
            ElasticsearchOssServer().use { es ->
                es.start()
                es.isRunning.shouldBeTrue()

                val config = ElasticsearchOssServer.Launcher.getClientConfiguration(es)
                assertCreateRestClient(config)
            }
        }
    }

    @Nested
    inner class UseDefaultPort {
        @Test
        fun `Elasticsearch OSS 서버를 기본 포트를 사용하여 실행하기`() {
            ElasticsearchOssServer(useDefaultPort = true).use { es ->
                es.start()
                es.isRunning.shouldBeTrue()
                es.port shouldBeEqualTo ElasticsearchOssServer.PORT

                val config = ElasticsearchOssServer.Launcher.getClientConfiguration(es)
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
