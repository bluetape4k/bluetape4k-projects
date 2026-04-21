package io.bluetape4k.mockwebflux

import io.bluetape4k.logging.KLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

/**
 * Mock WebFlux 서버 애플리케이션.
 * Spring Boot 4 WebFlux + Kotlin Coroutines 기반 테스트용 HTTP 서버.
 */
@EnableCaching
@SpringBootApplication
class MockWebfluxServerApplication {
    companion object: KLogging()
}

fun main(args: Array<String>) {
    runApplication<MockWebfluxServerApplication>(*args)
}
