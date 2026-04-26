package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.awt.image.BufferedImage
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoundedCornerFilterTest : AbstractFilterTest() {

    companion object : KLoggingChannel()

    /**
     * 256×256 흰색 이미지를 생성합니다.
     * 모든 픽셀은 불투명한 흰색(ARGB: 0xFFFFFFFF)입니다.
     */
    private fun createWhiteImage(): ImmutableImage {
        val buffered = BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)
        val g = buffered.createGraphics()
        try {
            g.color = java.awt.Color.WHITE
            g.fillRect(0, 0, 256, 256)
        } finally {
            g.dispose()
        }
        return ImmutableImage.fromAwt(buffered)
    }

    /**
     * radius=-1 은 허용 범위(0 이상) 밖이므로 [IllegalArgumentException]을 던져야 합니다.
     */
    @Test
    fun `radius=-1 은 IllegalArgumentException 을 던진다`() {
        assertThrows<IllegalArgumentException> {
            roundedCornerFilterOf(-1)
        }
    }

    /**
     * radius=0 은 필터를 적용하지 않으므로 원본과 동일한 이미지를 반환해야 합니다.
     */
    @Test
    fun `radius=0 은 원본과 동일한 이미지를 반환한다`() {
        val source = createWhiteImage()
        val result = source.copy().filter(roundedCornerFilterOf(0))
        assertSimilarToImage(result, source, tolerance = 0)
    }

    /**
     * radius=32 일 때 최외각 코너 픽셀(0,0)의 알파 값이 128 미만이어야 합니다.
     * (radius 바깥 픽셀은 투명하게 처리됨)
     */
    @Test
    fun `radius=32 는 코너 픽셀의 알파 값을 0 또는 매우 낮게 만든다`() {
        val source = createWhiteImage()
        val result = source.copy().filter(roundedCornerFilterOf(32))

        val cornerAlpha = (result.awt().getRGB(0, 0) ushr 24) and 0xFF
        cornerAlpha shouldBeLessThan 128
    }

    /**
     * radius=32 일 때 중앙 픽셀(128, 128)의 알파 값은 255 (완전 불투명)이어야 합니다.
     */
    @Test
    fun `radius=32 는 중앙 픽셀의 알파 값을 변경하지 않는다`() {
        val source = createWhiteImage()
        val result = source.copy().filter(roundedCornerFilterOf(32))

        val centerAlpha = (result.awt().getRGB(128, 128) ushr 24) and 0xFF
        centerAlpha shouldBeEqualTo 255
    }

    /**
     * 256×256 컬러 타일 이미지를 생성합니다 (골든 이미지 비교용).
     */
    private fun createColorTestImage(): ImmutableImage {
        val buffered = BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)
        val g = buffered.createGraphics()
        try {
            val colors = listOf(
                java.awt.Color.RED, java.awt.Color.GREEN, java.awt.Color.BLUE, java.awt.Color.YELLOW,
                java.awt.Color.CYAN, java.awt.Color.MAGENTA, java.awt.Color.ORANGE, java.awt.Color.PINK,
            )
            var idx = 0
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    g.color = colors[idx % colors.size]; idx++
                    g.fillRect(col * 64, row * 64, 64, 64)
                }
            }
        } finally {
            g.dispose()
        }
        return ImmutableImage.fromAwt(buffered)
    }

    /**
     * radius=32 적용 결과가 골든 이미지와 일치해야 합니다 (회귀 검증).
     */
    @Test
    fun `radius=32 결과는 골든 이미지와 일치한다`() {
        val result = createColorTestImage().filter(roundedCornerFilterOf(32))
        assertSimilarToResource(result, "expected_rounded_32.png", tolerance = 3)
    }
}
