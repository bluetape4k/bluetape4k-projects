package io.bluetape4k.spring.webflux.config

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.support.uninitialized
import io.bluetape4k.testcontainers.http.BluetapeHttpServer
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

/**
 * 커스텀 WebClient 설정 테스트.
 */
@SpringBootTest(classes = [CustomWebClientConfig::class])
class CustomWebClientConfigTest {
    companion object: KLoggingChannel() {
        private val httpbin by lazy { BluetapeHttpServer.Launcher.bluetapeHttpServer }
        private val httpbinUrl by lazy { httpbin.httpbinUrl }
    }

    @Autowired
    private val webClient: WebClient = uninitialized()

    @Autowired
    private val httpConnector: ReactorClientHttpConnector = uninitialized()

    @Test
    fun `create webclient using custom thread pool`() {
        httpConnector.shouldNotBeNull()
        webClient.shouldNotBeNull()
    }

    @Test
    fun `get by custom webclient`() =
        runSuspendIO {
            val response =
                webClient
                    .httpGet("$httpbinUrl/get")
                    .awaitBody<String>()

            // 로그 Thread name에 `web-client-thread-`가 있으면 custom thread pool을 사용한 것
            log.debug { "response=$response" }
        }

    @Test
    fun `async get by custom webclient`() =
        runSuspendIO {
            val task =
                List(2 * Runtimex.availableProcessors) {
                    async {
                        webClient
                            .httpGet("$httpbinUrl/get")
                            .awaitBody<String>()
                    }
                }
            task.awaitAll()
        }

    @Test
    fun `async get by custom webclient in multiple suspended jobs`() =
        runSuspendIO {
            SuspendedJobTester()
                .workers(Runtimex.availableProcessors)
                .rounds(Runtimex.availableProcessors)
                .add {
                    val body =
                        webClient
                            .httpGet("$httpbinUrl/get")
                            .awaitBody<String>()

                    log.debug { "httpbin get=${body.length}" }
                }.add {
                    val body =
                        webClient
                            .httpGet("$httpbinUrl/anything")
                            .awaitBody<String>()

                    log.debug { "httpbin anything=${body.length}" }
                }.add {
                    val body =
                        webClient
                            .httpGet("$httpbinUrl/headers")
                            .awaitBody<String>()

                    log.debug { "httpbin headers=${body.length}" }
                }.run()
        }
}
