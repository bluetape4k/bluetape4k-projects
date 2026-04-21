package io.bluetape4k.mockserver.httpbin

import io.bluetape4k.logging.KLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * httpbin 이미지 로더 서비스.
 *
 * 요청된 형식의 placeholder 이미지를 생성하고 캐시한다.
 * self-injection 없이 `@Cacheable`을 적용하기 위해 별도 Bean으로 분리.
 */
@Service
class ImageLoaderService {
    companion object : KLogging() {
        private val ALLOWED_FORMATS = setOf("png", "jpeg", "webp", "svg")
    }

    /**
     * 지정된 형식의 placeholder 이미지를 반환한다.
     *
     * PNG/JPEG는 100×100 픽셀 [BufferedImage]를 생성하고, SVG는 텍스트로 즉시 생성한다.
     * WEBP는 Java ImageIO 미지원으로 PNG로 폴백하여 반환한다.
     *
     * @param fmt 이미지 형식 (png, jpeg, webp, svg)
     * @return 이미지 바이트와 Content-Type을 포함한 [ResponseEntity]
     */
    @Cacheable("httpbin-image", key = "#fmt")
    fun loadImage(fmt: String): ResponseEntity<ByteArray> {
        return when (fmt.lowercase()) {
            "svg" -> {
                val svg = """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100">
  <rect width="100" height="100" fill="#cccccc"/>
  <text x="50" y="55" text-anchor="middle" font-size="12" fill="#666666">bluetape4k</text>
</svg>""".trimIndent()
                ResponseEntity.ok()
                    .header("Content-Type", "image/svg+xml")
                    .body(svg.toByteArray(Charsets.UTF_8))
            }
            "png", "jpeg", "webp" -> {
                // WEBP는 Java ImageIO 미지원 → PNG로 폴백
                val outputFmt = if (fmt == "webp") "png" else fmt
                val mediaType = if (fmt == "webp") "image/png" else "image/$fmt"
                val bytes = createPlaceholderImage(outputFmt)
                ResponseEntity.ok()
                    .header("Content-Type", mediaType)
                    .body(bytes)
            }
            else -> throw IllegalArgumentException("Unsupported image format: $fmt")
        }
    }

    /**
     * 100×100 픽셀 회색 placeholder 이미지를 생성한다.
     */
    private fun createPlaceholderImage(format: String): ByteArray {
        val image = BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB).apply {
            val g = createGraphics()
            g.color = java.awt.Color(200, 200, 200)
            g.fillRect(0, 0, 100, 100)
            g.dispose()
        }
        return ByteArrayOutputStream().use { baos ->
            ImageIO.write(image, format, baos)
            baos.toByteArray()
        }
    }
}
