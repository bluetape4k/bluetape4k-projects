package io.bluetape4k.mockserver

import io.bluetape4k.logging.KLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Bluetape4k Mock Server 애플리케이션.
 *
 * httpbin, jsonplaceholder 등 다양한 HTTP 테스트용 mock 엔드포인트를 제공하는 Spring Boot 애플리케이션.
 * 포트 8888에서 실행되며, Testcontainers 환경에서 외부 HTTP 의존성을 대체할 수 있다.
 */
@SpringBootApplication
class MockServerApplication {
    companion object : KLogging()
}

/**
 * 애플리케이션 진입점.
 */
fun main(args: Array<String>) {
    runApplication<MockServerApplication>(*args)
}
