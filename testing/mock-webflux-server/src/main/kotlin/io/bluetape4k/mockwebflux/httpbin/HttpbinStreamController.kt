package io.bluetape4k.mockwebflux.httpbin

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.json.JsonMapper
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

/**
 * httpbin.org 인코딩/스트리밍 엔드포인트 시뮬레이터 (WebFlux + Coroutines 버전).
 *
 * gzip/deflate 압축, NDJSON 스트리밍, 이미지 반환 엔드포인트를 제공한다.
 * 스트리밍은 [Flow]로 구현하여 backpressure를 자연스럽게 지원한다.
 *
 * ```kotlin
 * // baseUrl == "http://localhost:9999"
 * val client = WebClient.create("http://localhost:9999")
 * // NDJSON 스트리밍: 5개 JSON 라인 수신
 * val lines = client.get().uri("/httpbin/stream/5")
 *     .accept(MediaType.APPLICATION_NDJSON)
 *     .retrieve()
 *     .bodyToFlux<String>()
 *     .collectList()
 *     .awaitSingle()
 * // lines.size == 5
 * // gzip 압축 응답 수신
 * val gzipResp = client.get().uri("/httpbin/gzip")
 *     .retrieve().awaitBody<Map<String, Any>>()
 * ```
 */
@RestController
@RequestMapping("/httpbin")
class HttpbinStreamController(
    private val jsonMapper: JsonMapper,
    private val imageLoaderService: ImageLoaderService,
) {
    companion object: KLogging() {
        private val ALLOWED_IMAGE_FORMATS = setOf("png", "jpeg", "webp", "svg")
    }

    /**
     * gzip 압축된 httpbin 응답을 반환한다.
     *
     * 압축은 CPU + 메모리 집약 작업이지만 크기가 작으므로 동기로 수행한다.
     */
    @GetMapping("/gzip")
    suspend fun gzip(request: ServerHttpRequest): ResponseEntity<ByteArray> {
        val response = request.toHttpbinResponse(method = "GET").copy(
            json = mapOf("gzipped" to true)
        )
        val bytes = ByteArrayOutputStream().use { baos ->
            GZIPOutputStream(baos).use { gzip ->
                gzip.write(jsonMapper.writeValueAsBytes(response))
            }
            baos.toByteArray()
        }
        return ResponseEntity.ok()
            .header("Content-Encoding", "gzip")
            .header("Content-Type", "application/json")
            .body(bytes)
    }

    /**
     * deflate 압축된 httpbin 응답을 반환한다.
     */
    @GetMapping("/deflate")
    suspend fun deflate(request: ServerHttpRequest): ResponseEntity<ByteArray> {
        val response = request.toHttpbinResponse(method = "GET").copy(
            json = mapOf("deflated" to true)
        )
        val bytes = ByteArrayOutputStream().use { baos ->
            DeflaterOutputStream(baos).use { deflate ->
                deflate.write(jsonMapper.writeValueAsBytes(response))
            }
            baos.toByteArray()
        }
        return ResponseEntity.ok()
            .header("Content-Encoding", "deflate")
            .header("Content-Type", "application/json")
            .body(bytes)
    }

    /**
     * n개의 JSON 객체를 줄바꿈으로 구분해 스트리밍한다 (NDJSON).
     *
     * httpbin.org `/stream/{n}` 동작을 모사하며, 각 줄은 독립된 JSON 객체다.
     * WebFlux에서는 [Flow]<String> 반환 시 `application/x-ndjson` produces 와
     * 결합해 자동으로 줄 단위 스트리밍된다.
     *
     * ```kotlin
     * // baseUrl == "http://localhost:9999"
     * val client = WebClient.create("http://localhost:9999")
     * val lines = client.get().uri("/httpbin/stream/5")
     *     .accept(MediaType.APPLICATION_NDJSON)
     *     .retrieve()
     *     .bodyToFlux<String>()
     *     .collectList()
     *     .awaitSingle()
     * // lines.size == 5
     * ```
     *
     * @param n 이벤트 수 (1..100)
     */
    @GetMapping("/stream/{n}", produces = [MediaType.APPLICATION_NDJSON_VALUE])
    fun stream(@PathVariable n: Int): Flow<String> {
        require(n in 1..100) { "n must be 1..100, got: $n" }
        return flow {
            repeat(n) { i ->
                val line = jsonMapper.writeValueAsString(
                    mapOf("id" to i, "url" to "https://httpbin.org/stream/$n")
                )
                emit(line)
            }
        }.flowOn(Dispatchers.Default)
    }

    /**
     * 지정된 형식의 placeholder 이미지를 반환한다.
     *
     * 허용 형식: png, jpeg, webp, svg
     *
     * @param fmt 이미지 형식
     */
    @GetMapping("/image/{fmt}")
    suspend fun image(@PathVariable fmt: String): ResponseEntity<ByteArray> {
        fmt.requireNotBlank("fmt")
        require(fmt.lowercase() in ALLOWED_IMAGE_FORMATS) {
            "Unsupported image format: $fmt. Allowed: $ALLOWED_IMAGE_FORMATS"
        }
        return imageLoaderService.loadImage(fmt.lowercase())
    }
}
