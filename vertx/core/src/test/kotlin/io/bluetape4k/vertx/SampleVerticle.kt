package io.bluetape4k.vertx

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise

class SampleVerticle: AbstractVerticle() {

    companion object: KLoggingChannel() {
        const val PORT = 11981
    }

    override fun start(startPromise: Promise<Void>) {
        vertx.createHttpServer()
            .requestHandler { req ->
                log.debug { "Received request: $req" }
                req.response()
                    .putHeader("Content-Type", "text/plain")
                    .end("Yo!")
                log.info { "Handle a request on path ${req.path()} from ${req.remoteAddress().host()}" }
            }
            .listen(PORT)
            .onSuccess { server ->
                log.info { "Server is now listening! server actual port=${server.actualPort()}" }
                startPromise.complete()
            }
            .onFailure { error ->
                log.error(error) { "Failed to bind!" }
                startPromise.fail(error)
            }
    }
}
