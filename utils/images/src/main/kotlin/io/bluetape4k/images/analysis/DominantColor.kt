package io.bluetape4k.images.analysis

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val log = KotlinLogging.logger {}

/**
 * 이미지에서 추출된 대표 색상.
 *
 * @property r 빨간색 채널 (0..255)
 * @property g 녹색 채널 (0..255)
 * @property b 파란색 채널 (0..255)
 * @property population 이 색상이 속한 Median Cut 박스의 샘플 픽셀 수.
 *   [quality] 파라미터로 샘플링된 값이므로 원본 픽셀 수와 다를 수 있음.
 *   값이 클수록 해당 색상의 상대적 비중이 높음.
 */
data class DominantColor(
    val r: Int,
    val g: Int,
    val b: Int,
    val population: Int,
) {
    init {
        require(r in 0..255) { "r은 0..255 범위여야 합니다. 입력: $r" }
        require(g in 0..255) { "g는 0..255 범위여야 합니다. 입력: $g" }
        require(b in 0..255) { "b는 0..255 범위여야 합니다. 입력: $b" }
        require(population >= 0) { "population은 0 이상이어야 합니다. 입력: $population" }
    }

    /** `#rrggbb` 형식의 16진수 색상 코드 */
    val hex: String = "#%02x%02x%02x".format(r, g, b)

    /** java.awt.Color 변환 */
    fun toAwtColor(): java.awt.Color = java.awt.Color(r, g, b)

    companion object {
        /**
         * RGB 정수값(0xRRGGBB)으로부터 [DominantColor]를 생성한다.
         */
        fun fromRgb(rgb: Int, population: Int = 1): DominantColor =
            DominantColor(
                r = (rgb shr 16) and 0xFF,
                g = (rgb shr 8) and 0xFF,
                b = rgb and 0xFF,
                population = population,
            )
    }
}

/**
 * 이미지에서 대표 색상을 추출하는 전략 인터페이스.
 */
sealed interface DominantColorExtractor {

    /**
     * [image]에서 [count]개의 대표 색상을 추출한다.
     *
     * @param image 분석할 이미지
     * @param count 추출할 색상 수 (결과는 이 값보다 적을 수 있음 — 단색 이미지 등)
     * @return population 내림차순 정렬된 색상 목록
     */
    fun extract(image: ImmutableImage, count: Int): List<DominantColor>

    /**
     * Median Cut quantization 기반 추출 전략.
     *
     * color-thief-java (MIT) 알고리즘을 Kotlin으로 자체 구현.
     * 5-bit/channel (32 levels) RGB 색공간 사용.
     *
     * @property quality 픽셀 샘플링 간격 (1=모든 픽셀, 10=10픽셀당 1개). 범위: 1..30.
     * @property ignoreWhite true이면 R>250 && G>250 && B>250인 픽셀 제외.
     */
    data class MedianCut(
        val quality: Int = 10,
        val ignoreWhite: Boolean = false,
    ) : DominantColorExtractor {
        init {
            require(quality in 1..30) { "quality는 1..30 범위여야 합니다. 입력: $quality" }
        }

        override fun extract(image: ImmutableImage, count: Int): List<DominantColor> =
            MedianCutQuantizer.quantize(image, count, quality, ignoreWhite)
    }

    companion object {
        /** 기본 MedianCut 추출기를 생성한다. */
        fun medianCut(quality: Int = 10, ignoreWhite: Boolean = false): DominantColorExtractor =
            MedianCut(quality, ignoreWhite)
    }
}

/**
 * 이미지에서 [count]개의 대표 색상을 추출한다.
 *
 * @param count 추출할 색상 수. 결과 리스트 크기는 이 값 이하일 수 있음 (단색 이미지 등).
 * @param extractor 추출 전략. 기본값은 Median Cut.
 * @return population 내림차순 정렬된 색상 목록. 완전 투명 이미지의 경우 빈 리스트.
 */
fun ImmutableImage.dominantColors(
    count: Int = 5,
    extractor: DominantColorExtractor = DominantColorExtractor.medianCut(),
): List<DominantColor> {
    require(count >= 1) { "count는 1 이상이어야 합니다. 입력: $count" }
    return try {
        extractor.extract(this, count)
    } catch (e: Exception) {
        log.warn(e) { "대표 색상 추출 실패 (size=${width}x${height}, count=$count)" }
        emptyList()
    }
}

/**
 * 이미지에서 가장 지배적인 색상 1개를 추출한다.
 *
 * 완전 투명 이미지 등 색상 추출이 불가능한 경우 null을 반환한다.
 *
 * @param extractor 추출 전략. 기본값은 Median Cut.
 * @return 가장 대표적인 색상, 또는 추출 불가 시 null.
 */
fun ImmutableImage.dominantColor(
    extractor: DominantColorExtractor = DominantColorExtractor.medianCut(),
): DominantColor? = dominantColors(1, extractor).firstOrNull()

/**
 * 이미지에서 [count]개의 대표 색상을 비동기로 추출한다.
 *
 * CPU-bound 연산이므로 [Dispatchers.Default]를 사용한다.
 *
 * @param count 추출할 색상 수.
 * @param extractor 추출 전략.
 */
suspend fun ImmutableImage.suspendDominantColors(
    count: Int = 5,
    extractor: DominantColorExtractor = DominantColorExtractor.medianCut(),
): List<DominantColor> = withContext(Dispatchers.Default) { dominantColors(count, extractor) }
