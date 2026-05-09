package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients

@Suppress("DEPRECATION")
class ElasticsearchServerTest: AbstractContainerTest() {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {

        @Test
        fun `launch elasticsearch`() {
            ElasticsearchServer(reuse = false).use { es ->
                es.start()
                es.isRunning.shouldBeTrue()
            }
        }

        @Test
        fun `launch elastic search with ssl`() {
            ElasticsearchServer(reuse = false, password = "wow-world").use { es ->
                es.start()
                es.isRunning.shouldBeTrue()

                val config = ElasticsearchServer.Launcher.Spring.getClientConfiguration(es)
                ElasticsearchClients.createImperative(config).shouldNotBeNull()
            }
        }
    }

    @Nested
    @ResourceLock("elasticsearch-default-port")
    inner class UseDefaultPort {
        @Test
        fun `launch elasticsearch with default port`() {
            ElasticsearchServer(useDefaultPort = true, reuse = false).use { es ->
                es.start()
                es.isRunning.shouldBeTrue()
                es.port shouldBeEqualTo ElasticsearchServer.PORT
            }
        }
    }

    @Test
    fun `blank image tag 는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { ElasticsearchServer(image = " ") }
        assertFailsWith<IllegalArgumentException> { ElasticsearchServer(tag = " ") }
    }
}
