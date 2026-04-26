package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.filters.AbstractFilterTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

/**
 * 필터 체인 케이스의 골든 이미지 회귀 테스트.
 *
 * 각 테스트는 동일한 256×256 컬러 타일 이미지에 체인을 적용하고
 * 사전 생성된 골든 PNG(`src/test/resources/images/filters/expected_chain_*.png`)와 비교합니다.
 *
 * 골든 이미지 재생성: [GoldenImageGeneratorTest] 참조.
 */
class ChainGoldenImageTest : AbstractFilterTest() {

    companion object : KLoggingChannel()

    /** 256×256 컬러 타일 테스트 이미지 (GoldenImageGeneratorTest와 동일) */
    private fun createTestImage(): ImmutableImage {
        val buffered = BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB)
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
     * brightness → contrast → sepia 체인 출력이 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `brightness-contrast-sepia chain matches golden`() {
        val result = createTestImage().applyFilters {
            brightness(1.2f)
            contrast(1.1)
            sepia()
        }
        assertSimilarToResource(result, "expected_chain_brightness_contrast_sepia.png", tolerance = 3)
    }

    /**
     * saturation → hue → colorTemperature 체인 출력이 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `saturation-hue-colorTemperature chain matches golden`() {
        val result = createTestImage().applyFilters {
            saturation(1.3f)
            hue(30f)
            colorTemperature(5000)
        }
        assertSimilarToResource(result, "expected_chain_saturation_hue_temperature.png", tolerance = 3)
    }

    /**
     * grayscale → medianBlur → vignette 체인 출력이 골든 이미지와 일치해야 합니다.
     */
    @Test
    fun `grayscale-medianBlur-vignette chain matches golden`() {
        val result = createTestImage().applyFilters {
            grayscale()
            medianBlur(1)
            vignette()
        }
        assertSimilarToResource(result, "expected_chain_grayscale_median_vignette.png", tolerance = 3)
    }
}
