package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.vertx.core.Future
import kotlinx.atomicfu.atomic

class VertxHelloWorldService {

    companion object: KLoggingChannel()

    private val invocationCounter = atomic(0)
    val invocationCount by invocationCounter

    fun returnHelloWorld(): Future<String> {
        return Future.future { promise ->
            Thread.sleep(10)
            log.debug { "Execute returnHelloWorld" }
            invocationCounter.incrementAndGet()
            promise.complete("Hello world")
        }
    }

    fun throwException(): Future<String> = Future.future { promise ->
        Thread.sleep(10)
        log.debug { "Execute throwException" }
        invocationCounter.incrementAndGet()
        promise.fail(RuntimeException("Boom!"))
    }
}
