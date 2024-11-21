package io.bluetape4k.workshop.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.workshop.coroutines.handler.CoroutineHandler
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.server.coRouter

@SpringBootApplication
class CoroutineApplication {

    companion object: KLogging()

    @Bean
    fun routes(coroutineHandler: CoroutineHandler) = coRouter {
        GET("/", coroutineHandler::index)
        GET("/suspend", coroutineHandler::suspending)
        GET("/deferred", coroutineHandler::deferred)
        GET("/sequential-flow", coroutineHandler::sequentialFlow)
        GET("/concurrent-flow", coroutineHandler::concurrentFlow)
        GET("/error", coroutineHandler::error)
    }

}

fun main(args: Array<String>) {
    runApplication<CoroutineApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
