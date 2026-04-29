package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.awt.Color

/**
 * [mssimTo] 단위 테스트.
 *
 * MSSIM은 글로벌 [ssimTo]보다 국소 구조를 정확히 반영하므로, 같은 이미지 쌍에서 두 값은
 * 일반적으로 서로 다른 값을 가집니다.
 *
 * ⚠️ JPEG 압축·밝기 변화 임계값(0.7)은 임시값으로, T6-C calibration 단계에서
 * 실측값을 바탕으로 재조정합니다. 실제 측정값은 로그(`log.debug`)로 출력됩니다.
 */
class MssimSimilarityTest: AbstractImageTest() {

    companion object: KLoggingChannel() {
        private const val HOMER_JPG = "images/homer.jpg"
        private const val LABOR_JPG = "images/labor.jpg"
    }

    private fun loadImage(path: String): ImmutableImage =
        immutableImageOf(Resourcex.getInputStream(path)!!)

    @Test
    fun `identical images produce mssim near 1`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(HOMER_JPG)

        val score = a.mssimTo(b)
        log.debug { "MSSIM identical: $score" }

        score shouldBeGreaterThan 0.999
        score shouldBeInRange -1.0..1.0
    }

    @Test
    fun `different images produce mssim below 0_5`() {
        val homer = loadImage(HOMER_JPG)
        val landscape = loadImage(LANDSCAPE_JPG).scaleTo(homer.width, homer.height)

        val score = homer.mssimTo(landscape)
        log.debug { "MSSIM homer vs landscape: $score" }

        score shouldBeLessThan 0.5
        score shouldBeInRange -1.0..1.0
    }

    @Test
    fun `mssim differs from global ssim on the same image pair`() {
        val homer = loadImage(HOMER_JPG)
        val labor = loadImage(LABOR_JPG).scaleTo(homer.width, homer.height)

        val global = homer.ssimTo(labor)
        val mean = homer.mssimTo(labor)
        log.debug { "global SSIM=$global, MSSIM=$mean" }

        // 두 지표는 정의가 달라 동일 이미지 쌍에서도 다른 값을 가져야 합니다.
        mean shouldNotBeEqualTo global
    }

    @Test
    fun `even windowSize raises IllegalArgumentException`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(HOMER_JPG)

        assertThrows<IllegalArgumentException> {
            a.mssimTo(b, windowSize = 10)
        }
    }

    @Test
    fun `image smaller than window raises IllegalArgumentException`() {
        val tiny = loadImage(HOMER_JPG).scaleTo(7, 7)
        val tinyOther = loadImage(HOMER_JPG).scaleTo(7, 7)

        assertThrows<IllegalArgumentException> {
            tiny.mssimTo(tinyOther, windowSize = 11)
        }
    }

    @Test
    fun `jpeg quality 90 round-trip keeps mssim high`() {
        val original = loadImage(HOMER_JPG)
        val roundTrippedBytes = original.forWriter(JpegWriter(90, false)).bytes()
        val roundTripped = immutableImageOf(roundTrippedBytes)

        val score = original.mssimTo(roundTripped)
        log.debug { "MSSIM JPEG90: $score" }

        // 임시 임계값. T6-C calibration 단계에서 실측값으로 재조정.
        score shouldBeGreaterThan 0.7
        score shouldBeInRange -1.0..1.0
    }

    @Test
    fun `brightness shift +10 keeps mssim high`() {
        val original = loadImage(HOMER_JPG)
        val brighter = original.map { pixel ->
            val r = (pixel.red() + 10).coerceIn(0, 255)
            val g = (pixel.green() + 10).coerceIn(0, 255)
            val b = (pixel.blue() + 10).coerceIn(0, 255)
            Color(r, g, b, pixel.alpha())
        }

        val score = original.mssimTo(brighter)
        log.debug { "MSSIM brightness +10: $score" }

        // 임시 임계값. T6-C calibration 단계에서 실측값으로 재조정.
        score shouldBeGreaterThan 0.7
        score shouldBeInRange -1.0..1.0
    }
}
