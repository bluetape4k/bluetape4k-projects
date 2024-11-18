package io.bluetape4k.testcontainers.storage

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class ElasticsearchOssServerTest {

    companion object: KLogging()

    @Nested
    inner class UseDockerPort {
        @Test
        fun `Elasticsearch OSS 서버 실행`() {
            ElasticsearchOssServer().use { es ->
                es.start()
                es.isRunning.shouldBeTrue()
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
            }
        }
    }
}
