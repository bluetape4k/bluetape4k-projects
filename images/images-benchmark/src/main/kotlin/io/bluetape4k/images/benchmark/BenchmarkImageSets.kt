package io.bluetape4k.images.benchmark

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * JMH 벤치마크용 공용 이미지 세트 로더.
 *
 * `src/main/resources/bench/` 경로에서 이미지를 로드합니다.
 * 이미지가 없으면 합성 이미지를 생성합니다.
 *
 * 사용 예:
 * ```kotlin
 * val image = BenchmarkImageSets.photo4k
 * val doc = BenchmarkImageSets.document
 * val thumb = BenchmarkImageSets.thumbnail
 * ```
 */
object BenchmarkImageSets : KLogging() {

    /**
     * 리소스 경로에서 이미지를 로드하거나, 파일이 없으면 합성 이미지를 반환합니다.
     *
     * @param resourcePath 클래스패스 기준 리소스 경로 (예: `/bench/photo-4k.jpg`)
     * @param width 합성 이미지의 너비 (픽셀)
     * @param height 합성 이미지의 높이 (픽셀)
     * @return 로드 또는 합성된 [ImmutableImage]
     */
    private fun loadOrSynthesize(resourcePath: String, width: Int, height: Int): ImmutableImage {
        val stream = BenchmarkImageSets::class.java.getResourceAsStream(resourcePath)
        return if (stream != null) {
            log.debug { "벤치마크 이미지 로드: $resourcePath" }
            ImmutableImageLoader.create().fromStream(stream)
        } else {
            log.warn { "벤치마크 이미지를 찾을 수 없습니다: $resourcePath — 합성 이미지를 사용합니다 (${width}×${height})" }
            val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val g = buffered.createGraphics()
            g.color = Color(100, 150, 200)
            g.fillRect(0, 0, width, height)
            g.dispose()
            ImmutableImage.fromAwt(buffered)
        }
    }

    /** 4K 사진 이미지 (3840×2160). 리소스가 없으면 합성 이미지를 반환합니다. */
    val photo4k: ImmutableImage by lazy { loadOrSynthesize("/bench/photo-4k.jpg", 3840, 2160) }

    /** 문서 스캔 이미지 (1240×1754, A4). 리소스가 없으면 합성 이미지를 반환합니다. */
    val document: ImmutableImage by lazy { loadOrSynthesize("/bench/document.png", 1240, 1754) }

    /** 썸네일 이미지 (256×256). 리소스가 없으면 합성 이미지를 반환합니다. */
    val thumbnail: ImmutableImage by lazy { loadOrSynthesize("/bench/thumbnail.jpg", 256, 256) }
}
