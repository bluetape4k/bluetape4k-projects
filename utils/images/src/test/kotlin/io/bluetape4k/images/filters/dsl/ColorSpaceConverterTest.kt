package io.bluetape4k.images.filters.dsl

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlin.math.abs
import kotlin.random.Random
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.api.Test

class ColorSpaceConverterTest {

    companion object : KLoggingChannel()

    /**
     * RGB → HSV → RGB 라운드트립 퍼즈 테스트.
     * 1000회 무작위 RGB 값으로 변환 후 역변환하여 채널당 ±2 이하 오차를 검증합니다.
     */
    @Test
    fun `rgbToHsv and hsvToRgb roundtrip`() {
        val random = Random(42)
        repeat(1000) {
            val r = random.nextInt(256)
            val g = random.nextInt(256)
            val b = random.nextInt(256)
            val (h, s, v) = ColorSpaceConverter.rgbToHsv(r, g, b)
            val (r2, g2, b2) = ColorSpaceConverter.hsvToRgb(h, s, v)
            abs(r - r2).shouldBeLessOrEqualTo(2)
            abs(g - g2).shouldBeLessOrEqualTo(2)
            abs(b - b2).shouldBeLessOrEqualTo(2)
        }
    }

    /**
     * RGB → YCbCr → RGB 라운드트립 퍼즈 테스트.
     * 1000회 무작위 RGB 값으로 변환 후 역변환하여 채널당 ±3 이하 오차를 검증합니다.
     */
    @Test
    fun `rgbToYCbCr and yCbCrToRgb roundtrip`() {
        val random = Random(42)
        repeat(1000) {
            val r = random.nextInt(256)
            val g = random.nextInt(256)
            val b = random.nextInt(256)
            val (y, cb, cr) = ColorSpaceConverter.rgbToYCbCr(r, g, b)
            val (r2, g2, b2) = ColorSpaceConverter.yCbCrToRgb(y, cb, cr)
            abs(r - r2).shouldBeLessOrEqualTo(3)
            abs(g - g2).shouldBeLessOrEqualTo(3)
            abs(b - b2).shouldBeLessOrEqualTo(3)
        }
    }

    /**
     * kelvinToRgb 경계값 테스트.
     * 낮은 켈빈(KELVIN_MIN)은 붉은 색조, 높은 켈빈(KELVIN_MAX)은 푸른 색조를 검증합니다.
     */
    @Test
    fun `kelvinToRgb boundary values`() {
        val (r1000, g1000, b1000) = ColorSpaceConverter.kelvinToRgb(ColorSpaceConverter.KELVIN_MIN)
        // 낮은 kelvin → 붉은: R가 G, B보다 크거나 같음
        r1000 shouldBeGreaterOrEqualTo g1000
        r1000 shouldBeGreaterOrEqualTo b1000

        val (r40000, _, b40000) = ColorSpaceConverter.kelvinToRgb(ColorSpaceConverter.KELVIN_MAX)
        // 높은 kelvin → 푸른: B가 R보다 크거나 같음
        b40000 shouldBeGreaterOrEqualTo r40000
    }

    /**
     * kelvinToRgb 범위 클램프 테스트.
     * KELVIN_MIN 미만 및 KELVIN_MAX 초과 입력이 경계값과 동일한 결과를 반환하는지 검증합니다.
     */
    @Test
    fun `kelvinToRgb clamps out-of-range input`() {
        ColorSpaceConverter.kelvinToRgb(500) shouldBeEqualTo ColorSpaceConverter.kelvinToRgb(ColorSpaceConverter.KELVIN_MIN)
        ColorSpaceConverter.kelvinToRgb(50000) shouldBeEqualTo ColorSpaceConverter.kelvinToRgb(ColorSpaceConverter.KELVIN_MAX)
    }

    /**
     * rgbToHsvInto 함수가 rgbToHsv Triple 반환 버전과 동일한 결과를 내는지 검증합니다.
     */
    @Test
    fun `rgbToHsvInto matches rgbToHsv`() {
        val out = FloatArray(3)
        ColorSpaceConverter.rgbToHsvInto(255, 128, 0, out)
        val (h, s, v) = ColorSpaceConverter.rgbToHsv(255, 128, 0)
        out[0] shouldBeEqualTo h
        out[1] shouldBeEqualTo s
        out[2] shouldBeEqualTo v
    }

    /**
     * kelvinToRgbInto 함수가 kelvinToRgb Triple 반환 버전과 동일한 결과를 내는지 검증합니다.
     */
    @Test
    fun `kelvinToRgbInto matches kelvinToRgb`() {
        val out = IntArray(3)
        ColorSpaceConverter.kelvinToRgbInto(5500, out)
        val (r, g, b) = ColorSpaceConverter.kelvinToRgb(5500)
        out[0] shouldBeEqualTo r
        out[1] shouldBeEqualTo g
        out[2] shouldBeEqualTo b
    }
}
