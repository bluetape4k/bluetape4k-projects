package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.filters.dsl.ColorSpaceConverter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.awt.image.BufferedImage
import kotlin.math.abs
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SaturationFilterTest : AbstractFilterTest() {

    companion object : KLoggingChannel()

    /**
     * 결정론적 컬러 타일 이미지(256×256)를 생성합니다.
     * 4×4 타일로 구성되며, 각 타일은 고정된 색상 목록에서 순환 배정됩니다.
     */
    private fun createTestImage(): ImmutableImage {
        val buffered = BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB)
        val g = buffered.createGraphics()
        try {
            val colors = listOf(
                java.awt.Color.RED, java.awt.Color.GREEN, java.awt.Color.BLUE, java.awt.Color.YELLOW,
                java.awt.Color.CYAN, java.awt.Color.MAGENTA, java.awt.Color.ORANGE, java.awt.Color.PINK,
            )
            var colorIdx = 0
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    g.color = colors[colorIdx % colors.size]
                    colorIdx++
                    g.fillRect(col * 64, row * 64, 64, 64)
                }
            }
        } finally {
            g.dispose()
        }
        return ImmutableImage.fromAwt(buffered)
    }

    /**
     * factor=1.0 일 때 채도를 변경하지 않으므로 원본과 거의 동일한 이미지를 반환해야 합니다.
     */
    @Test
    fun `factor=1_0 은 원본과 유사한 이미지를 반환한다`() {
        val source = createTestImage()
        val result = source.copy().filter(saturationFilterOf(1.0f))
        assertSimilarToImage(result, source, tolerance = 2)
    }

    /**
     * factor=0 일 때 모든 픽셀의 채도가 0이 되어 R=G=B (회색) 이어야 합니다.
     * 허용 오차 2 이내에서 R, G, B 채널 값이 서로 같은지 검증합니다.
     */
    @Test
    fun `factor=0 은 흑백 이미지를 반환한다`() {
        val source = createTestImage()
        val result = source.copy().filter(saturationFilterOf(0f))
        val pixels = result.pixels()
        for (pixel in pixels) {
            val r = pixel.red()
            val g = pixel.green()
            val b = pixel.blue()
            require(abs(r - g) <= 2) { "픽셀 R=$r, G=$g 값이 tolerance=2를 초과: ${abs(r - g)}" }
            require(abs(r - b) <= 2) { "픽셀 R=$r, B=$b 값이 tolerance=2를 초과: ${abs(r - b)}" }
            require(abs(g - b) <= 2) { "픽셀 G=$g, B=$b 값이 tolerance=2를 초과: ${abs(g - b)}" }
        }
    }

    /**
     * factor가 음수이면 [IllegalArgumentException]을 던져야 합니다.
     */
    @Test
    fun `factor=-0_1 은 IllegalArgumentException 을 던진다`() {
        assertThrows<IllegalArgumentException> {
            saturationFilterOf(-0.1f)
        }
    }

    /**
     * factor=1.5 적용 결과가 골든 이미지와 일치해야 합니다 (회귀 검증).
     */
    @Test
    fun `factor=1_5 결과는 골든 이미지와 일치한다`() {
        val result = createTestImage().copy().filter(saturationFilterOf(1.5f))
        assertSimilarToResource(result, "expected_saturation_1_5.png", tolerance = 3)
    }

    /**
     * factor=1.5 일 때 원본보다 평균 채도(HSV S 채널)가 높아야 합니다.
     */
    @Test
    fun `factor=1_5 는 원본보다 채도가 높은 이미지를 반환한다`() {
        val source = createTestImage()
        val result = source.copy().filter(saturationFilterOf(1.5f))

        val sourcePixels = source.pixels()
        val resultPixels = result.pixels()

        val hsvOut = FloatArray(3)

        var sourceSatSum = 0.0
        var resultSatSum = 0.0

        for (i in sourcePixels.indices) {
            val sp = sourcePixels[i]
            ColorSpaceConverter.rgbToHsvInto(sp.red(), sp.green(), sp.blue(), hsvOut)
            sourceSatSum += hsvOut[1]

            val rp = resultPixels[i]
            ColorSpaceConverter.rgbToHsvInto(rp.red(), rp.green(), rp.blue(), hsvOut)
            resultSatSum += hsvOut[1]
        }

        val sourceMean = sourceSatSum / sourcePixels.size
        val resultMean = resultSatSum / resultPixels.size

        require(resultMean >= sourceMean) {
            "결과 이미지의 평균 채도($resultMean)가 원본($sourceMean)보다 작습니다."
        }
    }
}
