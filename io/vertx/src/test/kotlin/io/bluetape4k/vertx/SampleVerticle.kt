package io.bluetape4k.vertx

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise

class SampleVerticle(private val port: Int = 0): AbstractVerticle() {

    companion object: KLoggingChannel()

    /**
     * 서버가 바인딩된 실제 포트. [start] 완료 후 사용 가능.
     */
    var actualPort: Int = 0
        private set

    override fun start(startPromise: Promise<Void>) {
        vertx.createHttpServer()
            .requestHandler { req ->
                log.debug { "Received request: $req" }
                req.response()
                    .putHeader("Content-Type", "text/plain")
                    .end("Yo!")
                log.info { "Handle a request on path ${req.path()} from ${req.remoteAddress().host()}" }
            }
            .listen(port)
            .onSuccess { server ->
                actualPort = server.actualPort()
                log.info { "Server is now listening! server actual port=$actualPort" }
                startPromise.complete()
            }
            .onFailure { error ->
                log.error(error) { "Failed to bind!" }
                startPromise.fail(error)
            }
    }
}
