package io.bluetape4k.workshop.coroutines.controller

import com.fasterxml.jackson.databind.JsonNode
import io.bluetape4k.coroutines.flow.extensions.flowRangeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.uninitialized
import io.bluetape4k.workshop.coroutines.model.Banner
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@RestController
@RequestMapping("/controller")
class CoroutineController(
    @Autowired private val builder: WebClient.Builder,
): CoroutineScope by CoroutineScope(Dispatchers.IO + CoroutineName("controller")) {

    companion object: KLogging() {
        private const val DEFAULT_DELAY = 1000L
    }

    @Value("\${server.port:8080}")
    private val port: String = uninitialized()

    // 응답용 객체
    private val banner = Banner("제목", "동해물과 백두산이 마르고 닳도록")

    // API Server에서 다른 API 서버를 호출하는 것을 흉내내기 위해서 사용합니다.
    private val client by lazy { builder.baseUrl("http://localhost:$port/controller").build() }

    private fun currentCoroutineName(): String? = coroutineContext[CoroutineName]?.name

    @GetMapping(value = ["/", "/index"])
    suspend fun render(model: Model): Banner {
        delay(DEFAULT_DELAY)
        return banner
    }

    /**
     * suspend 함수를 이용하여, 비동기 방식으로 실행합니다.
     */
    @GetMapping("/suspend")
    suspend fun suspendingEndpoint(): Banner {
        delay(DEFAULT_DELAY)
        log.debug { "coroutineName=[${currentCoroutineName()}]" }
        log.info { "Suspending... return $banner" }
        return banner
    }

    /**
     * Deferred (CompletableFuture와 유사) 객체를 반환하여 비동기 실행을 수행합니다.
     */
    @GetMapping("/deferred")
    fun deferredEndpoint(): Deferred<Banner> = async {
        delay(DEFAULT_DELAY)
        log.debug { "coroutineName=[${currentCoroutineName()}]" }
        log.info { "Deferred ... return $banner" }
        banner
    }

    @GetMapping("/sequential-flow")
    fun sequentialFlow(): Flow<Banner> {
        log.info { "Get banners in sequential mode." }
        return flow {
            repeat(4) {
                log.debug { "coroutineName=[${currentCoroutineName()}]" }
                emit(getBanner())
            }
        }
    }

    @GetMapping("/concurrent-flow")
    fun concurrentFlow(): Flow<Banner> {
        log.info { "Get banners in concurrent mode." }

        return flowRangeOf(0, 4).flatMapMerge {
            log.debug { "coroutineName=[${currentCoroutineName()}]" }
            flow { emit(getBanner()) }
        }
    }

    @GetMapping("/error")
    suspend fun error() {
        throw RuntimeException("Boom!")
    }

    @PostMapping("/request-as-flow")
    fun requestAsStream(@RequestBody requests: Flow<JsonNode>): Flow<String> {
        return flow {
            requests.collect { jsonNode ->
                log.debug { "jsonNode=${jsonNode.toPrettyString()}, coroutineName=[${currentCoroutineName()}]" }
                emit(jsonNode.toPrettyString())
            }
        }
    }

    private suspend fun getBanner(): Banner {
        log.debug { "coroutineName=[${currentCoroutineName()}]" }
        log.debug { "Get banner from `/controller/suspend` " }
        return client.get().uri("/suspend").accept(MediaType.APPLICATION_JSON).retrieve().awaitBody()
    }
}
