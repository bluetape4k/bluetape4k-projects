package io.bluetape4k.workshop.coroutines.handler

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.uninitialized
import io.bluetape4k.workshop.coroutines.model.Banner
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyAndAwait
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.renderAndAwait

@Component
class CoroutineHandler(
    @Autowired private val builder: WebClient.Builder,
): CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineName("handler")) {

    companion object: KLogging()

    @Value("\${server.port:8080}")
    private val port: String = uninitialized()

    private val client: WebClient by lazy { builder.baseUrl("http://localhost:$port").build() }

    private val banner = Banner("제목", "동해물과 백두산이 마르고 닳도록")

    private suspend fun currentCoroutineName(): String? = coroutineContext[CoroutineName]?.name

    suspend fun index(request: ServerRequest): ServerResponse {
        return ServerResponse.ok()
            .renderAndAwait("index", mapOf("banner" to banner))
    }


    suspend fun suspending(request: ServerRequest): ServerResponse {
        delay(10)
        log.info { "Suspending... return $banner" }
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(banner)
    }

    suspend fun deferred(request: ServerRequest): ServerResponse = coroutineScope {
        val body = async {
            delay(10)
            banner
        }

        log.info { "Deferred... return $banner" }

        ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(body.await())
    }

    suspend fun sequentialFlow(request: ServerRequest): ServerResponse {
        log.info { "Get banners in sequential mode." }

        val flow = flow {
            repeat(4) {
                emit(getBanner())
            }
        }

        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(flow)
    }

    suspend fun concurrentFlow(request: ServerRequest): ServerResponse {
        log.info { "Get banners in concurrent mode." }

        val flow = (0..3).asFlow()
            .flatMapMerge {
                flow { emit(getBanner()) }
            }

        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(flow)
    }

    private suspend fun getBanner(): Banner {
        log.debug { "Get banner from `/suspend` " }

        return client.get()
            .uri("/suspend")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .awaitBody()
    }

    suspend fun error(request: ServerRequest): ServerResponse {
        throw RuntimeException("Boom!")
    }
}
