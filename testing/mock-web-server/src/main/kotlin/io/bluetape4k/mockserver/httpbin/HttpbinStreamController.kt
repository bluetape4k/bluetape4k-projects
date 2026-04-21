package io.bluetape4k.mockserver.httpbin

import io.bluetape4k.logging.KLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.json.JsonMapper
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

/**
 * httpbin.org 인코딩/스트리밍 엔드포인트 시뮬레이터.
 *
 * gzip/deflate 압축, NDJSON 스트리밍, 이미지 반환 엔드포인트를 제공한다.
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
     * n개의 JSON 객체를 줄바꿈으로 구분해 스트리밍한다 (NDJSON).
     *
     * httpbin.org `/stream/{n}` 동작을 모사하며, 각 줄은 독립된 JSON 객체다.
     * AsyncJsonParser 등 스트리밍 파서와 함께 사용할 수 있다.
     *
     * @param n 이벤트 수 (1..100)
     */
    @GetMapping("/stream/{n}", produces = ["application/x-ndjson"])
    fun stream(@PathVariable n: Int, response: HttpServletResponse) {
        require(n in 1..100) { "n must be 1..100, got: $n" }
        response.contentType = "application/x-ndjson"
        response.writer.use { writer ->
            repeat(n) { i ->
                val line = jsonMapper.writeValueAsString(mapOf("id" to i, "url" to "https://httpbin.org/stream/$n"))
                writer.println(line)
                writer.flush()
            }
        }
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
