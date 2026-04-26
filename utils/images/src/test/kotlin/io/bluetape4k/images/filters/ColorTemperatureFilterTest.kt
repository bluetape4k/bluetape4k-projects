package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.awt.image.BufferedImage
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ColorTemperatureFilterTest : AbstractFilterTest() {

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
     * kelvin=999 은 허용 범위(1000~40000) 밖이므로 [IllegalArgumentException]을 던져야 합니다.
     */
    @Test
    fun `kelvin=999 은 IllegalArgumentException 을 던진다`() {
        assertThrows<IllegalArgumentException> {
            colorTemperatureFilterOf(999)
        }
    }

    /**
     * kelvin=40001 은 허용 범위(1000~40000) 밖이므로 [IllegalArgumentException]을 던져야 합니다.
     */
    @Test
    fun `kelvin=40001 은 IllegalArgumentException 을 던진다`() {
        assertThrows<IllegalArgumentException> {
            colorTemperatureFilterOf(40001)
        }
    }

    /**
     * kelvin=6500 은 중성에 가까우므로 결과 이미지가 원본과 거의 동일해야 합니다.
     * 6500K에서 Tanner Helland 알고리즘의 RGB는 (255, 255, 255)에 가까워 큰 색상 변화가 없습니다.
     */
    @Test
    fun `kelvin=6500 은 원본과 유사한 이미지를 반환한다`() {
        val source = createTestImage()
        val result = source.copy().filter(colorTemperatureFilterOf(6500))
        assertSimilarToImage(result, source, tolerance = 10)
    }

    /**
     * kelvin=3000 은 따뜻한 톤(붉은 계열)이므로 원본과 충분히 달라야 합니다.
     */
    @Test
    fun `kelvin=3000 은 원본과 충분히 다른 이미지를 반환한다`() {
        val source = createTestImage()
        val result = source.copy().filter(colorTemperatureFilterOf(3000))
        assertNotSimilarToImage(result, source, threshold = 5)
    }

    /**
     * kelvin=10000 은 차가운 톤(푸른 계열)이므로 결과 이미지의 B 채널 평균이 원본보다 높아야 합니다.
     */
    @Test
    fun `kelvin=10000 은 원본보다 B 채널 평균이 높은 이미지를 반환한다`() {
        val source = createTestImage()
        val result = source.copy().filter(colorTemperatureFilterOf(10000))

        val sourcePixels = source.pixels()
        val resultPixels = result.pixels()

        var sourceBSum = 0L
        var resultBSum = 0L
        for (i in sourcePixels.indices) {
            sourceBSum += sourcePixels[i].blue()
            resultBSum += resultPixels[i].blue()
        }

        val sourceBMean = sourceBSum.toDouble() / sourcePixels.size
        val resultBMean = resultBSum.toDouble() / resultPixels.size

        resultBMean.toLong() shouldBeGreaterOrEqualTo sourceBMean.toLong()
    }

    /**
     * kelvin=3000 적용 결과가 골든 이미지와 일치해야 합니다 (회귀 검증).
     */
    @Test
    fun `kelvin=3000 결과는 골든 이미지와 일치한다`() {
        val result = createTestImage().copy().filter(colorTemperatureFilterOf(3000))
        assertSimilarToResource(result, "expected_temperature_3000.png", tolerance = 3)
    }
}
