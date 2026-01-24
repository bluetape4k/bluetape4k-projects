package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.vertx.core.Future
import java.util.concurrent.atomic.AtomicInteger

class VertxHelloWorldService {

    companion object: KLoggingChannel()

    val invocationCounter = AtomicInteger(0)
    val invocationCount get() = invocationCounter.get()

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
