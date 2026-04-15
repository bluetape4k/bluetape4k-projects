package io.bluetape4k.mockserver.httpbin

import io.bluetape4k.logging.KLogging
import tools.jackson.databind.json.JsonMapper
import io.bluetape4k.logging.warn
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

/**
 * httpbin.org 인코딩/스트리밍 엔드포인트 시뮬레이터.
 *
 * gzip/deflate 압축, SSE 스트리밍, 이미지 반환 엔드포인트를 제공한다.
 */
@RestController
@RequestMapping("/httpbin")
class HttpbinStreamController(
    private val jsonMapper: JsonMapper,
    private val imageLoaderService: ImageLoaderService,
) {
    companion object : KLogging() {
        private val ALLOWED_IMAGE_FORMATS = setOf("png", "jpeg", "webp", "svg")
    }

    /**
     * gzip 압축된 httpbin 응답을 반환한다.
     */
    @GetMapping("/gzip")
    fun gzip(request: HttpServletRequest): ResponseEntity<ByteArray> {
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
    fun deflate(request: HttpServletRequest): ResponseEntity<ByteArray> {
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
     * n개의 SSE 이벤트를 순차적으로 전송한다.
     *
     * @param n 이벤트 수 (1..100)
     */
    @GetMapping("/stream/{n}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(@PathVariable n: Int): SseEmitter {
        require(n in 1..100) { "n must be 1..100, got: $n" }
        val emitter = SseEmitter(30_000L)
        Thread.ofVirtual().start {
            try {
                repeat(n) { i ->
                    emitter.send(
                        SseEmitter.event()
                            .id(i.toString())
                            .data(mapOf("id" to i, "url" to "https://httpbin.org/stream/$n"))
                    )
                }
                emitter.complete()
            } catch (e: Exception) {
                log.warn(e) { "SSE stream error" }
                emitter.completeWithError(e)
            }
        }
        return emitter
    }

    /**
     * 지정된 형식의 placeholder 이미지를 반환한다.
     *
     * 허용 형식: png, jpeg, webp, svg
     *
     * @param fmt 이미지 형식
     */
    @GetMapping("/image/{fmt}")
    fun image(@PathVariable fmt: String): ResponseEntity<ByteArray> {
        require(fmt.lowercase() in ALLOWED_IMAGE_FORMATS) {
            "Unsupported image format: $fmt. Allowed: $ALLOWED_IMAGE_FORMATS"
        }
        return imageLoaderService.loadImage(fmt.lowercase())
    }
}
