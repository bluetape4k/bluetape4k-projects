package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.awt.image.BufferedImage
import org.junit.jupiter.api.Test

class HueFilterTest : AbstractFilterTest() {

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
     * delta=0 일 때 색조를 변경하지 않으므로 원본과 거의 동일한 이미지를 반환해야 합니다.
     */
    @Test
    fun `delta=0 은 원본과 유사한 이미지를 반환한다`() {
        val source = createTestImage()
        val result = source.copy().filter(hueFilterOf(0f))
        assertSimilarToImage(result, source, tolerance = 2)
    }

    /**
     * delta=360 일 때 360도 회전은 원본과 동일해야 합니다 (모듈러 연산).
     */
    @Test
    fun `delta=360 은 원본과 유사한 이미지를 반환한다`() {
        val source = createTestImage()
        val result = source.copy().filter(hueFilterOf(360f))
        assertSimilarToImage(result, source, tolerance = 2)
    }

    /**
     * delta=-360 일 때 -360도 회전도 원본과 동일해야 합니다 (모듈러 연산).
     */
    @Test
    fun `delta=-360 은 원본과 유사한 이미지를 반환한다`() {
        val source = createTestImage()
        val result = source.copy().filter(hueFilterOf(-360f))
        assertSimilarToImage(result, source, tolerance = 2)
    }

    /**
     * delta=180 일 때 색조를 반전하므로 원본과 충분히 달라야 합니다.
     */
    @Test
    fun `delta=180 은 원본과 충분히 다른 이미지를 반환한다`() {
        val source = createTestImage()
        val result = source.copy().filter(hueFilterOf(180f))
        assertNotSimilarToImage(result, source, threshold = 5)
    }

    /**
     * delta=60 적용 결과가 골든 이미지와 일치해야 합니다 (회귀 검증).
     */
    @Test
    fun `delta=60 결과는 골든 이미지와 일치한다`() {
        val result = createTestImage().copy().filter(hueFilterOf(60f))
        assertSimilarToResource(result, "expected_hue_60.png", tolerance = 3)
    }
}
