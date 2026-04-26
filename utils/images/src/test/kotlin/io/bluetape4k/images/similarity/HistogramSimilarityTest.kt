package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test

class HistogramSimilarityTest: AbstractImageTest() {

    companion object: KLoggingChannel() {
        private const val HOMER_JPG = "images/homer.jpg"
        private const val LANDSCAPE_JPG = "images/landscape.jpg"
    }

    private fun loadImage(path: String): ImmutableImage =
        immutableImageOf(Resourcex.getInputStream(path)!!)

    @Test
    fun `identical images yield similarity close to 1 across all measures and color spaces`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(HOMER_JPG)

        // 동일 이미지는 모든 측정에서 1.0에 매우 가깝게 나와야 한다.
        val measures = listOf(
            HistogramSimilarity.chiSquare(ColorSpace.RGB),
            HistogramSimilarity.chiSquare(ColorSpace.HSV),
            HistogramSimilarity.bhattacharyya(ColorSpace.RGB),
            HistogramSimilarity.bhattacharyya(ColorSpace.HSV),
            HistogramSimilarity.earthMover(ColorSpace.RGB),
            HistogramSimilarity.earthMover(ColorSpace.HSV),
        )

        measures.forEach { measure ->
            val score = measure.measure(a, b)
            log.debug { "identical($measure) = $score" }
            score shouldBeGreaterThan 0.95
        }
    }

    @Test
    fun `different images produce lower similarity than identical images across all measures`() {
        val homer = loadImage(HOMER_JPG)
        val landscape = loadImage(LANDSCAPE_JPG)

        // 측정별 임계값:
        // - ChiSquare: 분포 차이에 매우 민감 (< 0.7)
        // - EarthMover: CDF 누적 차이라 자연 이미지에선 둔감 (< 0.9)
        // - Bhattacharyya: 채널 평균/제곱근으로 자연 이미지에선 둔감 (< 0.9)
        val expectations = listOf(
            HistogramSimilarity.chiSquare() to 0.7,
            HistogramSimilarity.earthMover() to 0.9,
            HistogramSimilarity.bhattacharyya() to 0.9,
        )

        expectations.forEach { (measure, upperBound) ->
            val score = measure.measure(homer, landscape)
            log.debug { "different($measure) = $score (upperBound=$upperBound)" }
            score shouldBeLessThan upperBound
        }
    }

    @Test
    fun `images with different sizes are accepted without throwing`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(HOMER_JPG).scaleTo(200, 150)

        // 크기 mismatch에도 예외가 발생하지 않고 [0,1] 범위의 결과를 돌려주어야 한다.
        val chi = HistogramSimilarity.chiSquare().measure(a, b)
        val bha = HistogramSimilarity.bhattacharyya().measure(a, b)
        val emd = HistogramSimilarity.earthMover().measure(a, b)

        chi shouldBeInRange 0.0..1.0
        bha shouldBeInRange 0.0..1.0
        emd shouldBeInRange 0.0..1.0
    }

    @Test
    fun `same image at different size yields high similarity`() {
        val a = loadImage(HOMER_JPG)
        val b = a.scaleTo(100, 100)

        // 강한 다운스케일은 보간으로 색상 분포를 흐트러뜨리므로 측정별 임계값을 다르게 둔다.
        // - ChiSquare: 분포 차이에 민감해 0.85 정도가 현실적
        // - EarthMover: CDF 누적이라 오히려 견고 (>= 0.95)
        // - Bhattacharyya: 채널 평균/제곱근으로 안정적 (>= 0.95)
        val expectations = listOf(
            HistogramSimilarity.chiSquare() to 0.85,
            HistogramSimilarity.earthMover() to 0.95,
            HistogramSimilarity.bhattacharyya() to 0.95,
        )

        expectations.forEach { (measure, lowerBound) ->
            val score = measure.measure(a, b)
            log.debug { "scaled-same($measure) = $score (lowerBound=$lowerBound)" }
            score shouldBeGreaterThan lowerBound
        }
    }

    @Test
    fun `RGB and HSV produce different similarity scores for the same image pair`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(LANDSCAPE_JPG)

        val rgb = HistogramSimilarity.chiSquare(ColorSpace.RGB).measure(a, b)
        val hsv = HistogramSimilarity.chiSquare(ColorSpace.HSV).measure(a, b)

        log.debug { "RGB=$rgb, HSV=$hsv" }

        // 부동소수 비교는 절대값 차이가 충분히 큰지 확인 (정확한 일치 비교는 부서지기 쉬움).
        kotlin.math.abs(rgb - hsv) shouldBeGreaterThan 1e-6
    }

    @Test
    fun `binsPerChannel out of range throws IllegalArgumentException`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(HOMER_JPG)

        invoking {
            HistogramSimilarity.chiSquare(bins = 1).measure(a, b)
        } shouldThrow IllegalArgumentException::class

        invoking {
            HistogramSimilarity.bhattacharyya(bins = 257).measure(a, b)
        } shouldThrow IllegalArgumentException::class

        invoking {
            HistogramSimilarity.earthMover(bins = 0).measure(a, b)
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `histogramSimilarityTo extension yields similarity above 0_95 for identical image`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(HOMER_JPG)

        val score = a.histogramSimilarityTo(b)
        log.debug { "extension default = $score" }

        score shouldBeGreaterThan 0.95
    }
}
