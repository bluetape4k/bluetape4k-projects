package io.bluetape4k.mockwebflux

import io.bluetape4k.logging.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * mock-webflux-server 통합 테스트 베이스 클래스.
 * WebTestClient 를 사용하여 WebFlux 엔드포인트를 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
abstract class AbstractMockWebfluxServerTest {
    companion object: KLogging()

    @Autowired
    protected lateinit var client: WebTestClient
}
