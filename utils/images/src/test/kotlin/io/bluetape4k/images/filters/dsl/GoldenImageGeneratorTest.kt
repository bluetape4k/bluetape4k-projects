package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import io.bluetape4k.images.filters.AbstractFilterTest
import io.bluetape4k.images.filters.colorTemperatureFilterOf
import io.bluetape4k.images.filters.hueFilterOf
import io.bluetape4k.images.filters.medianBlurFilterOf
import io.bluetape4k.images.filters.roundedCornerFilterOf
import io.bluetape4k.images.filters.saturationFilterOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.nio.file.Paths

/**
 * 신규 필터 및 체인 케이스의 골든 이미지를 생성합니다.
 *
 * 이 클래스는 평상시 @Disabled 상태입니다.
 * 필터 구현이 변경되어 골든 이미지를 재생성해야 할 때만 @Disabled를 제거하고 실행하세요.
 *
 * 실행 후 `src/test/resources/images/filters/` 에 PNG 파일이 생성됩니다.
 */
@Disabled("골든 이미지 재생성 시에만 활성화")
class GoldenImageGeneratorTest : AbstractFilterTest() {

    companion object : KLoggingChannel() {
        // Gradle 테스트 working directory = utils/images/
        private val RESOURCE_DIR = Paths.get("src/test/resources/images/filters")
    }

    /** 256×256 컬러 타일 테스트 이미지 */
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

    private fun ImmutableImage.savePng(filename: String) {
        val path = RESOURCE_DIR.resolve(filename)
        path.parent.toFile().mkdirs()
        this.forWriter(PngWriter.MaxCompression).write(path)
        log.info("저장: $path")
    }

    @Test
    fun `generate saturation golden`() {
        createTestImage().filter(saturationFilterOf(1.5f)).savePng("expected_saturation_1_5.png")
    }

    @Test
    fun `generate hue golden`() {
        createTestImage().filter(hueFilterOf(60f)).savePng("expected_hue_60.png")
    }

    @Test
    fun `generate color temperature golden`() {
        createTestImage().filter(colorTemperatureFilterOf(3000)).savePng("expected_temperature_3000.png")
    }

    @Test
    fun `generate rounded corner golden`() {
        createTestImage().filter(roundedCornerFilterOf(32)).savePng("expected_rounded_32.png")
    }

    @Test
    fun `generate median blur golden`() {
        createTestImage().filter(medianBlurFilterOf(2)).savePng("expected_median_2.png")
    }

    @Test
    fun `generate chain brightness-contrast-sepia golden`() {
        createTestImage().applyFilters {
            brightness(1.2f)
            contrast(1.1)
            sepia()
        }.savePng("expected_chain_brightness_contrast_sepia.png")
    }

    @Test
    fun `generate chain saturation-hue-temperature golden`() {
        createTestImage().applyFilters {
            saturation(1.3f)
            hue(30f)
            colorTemperature(5000)
        }.savePng("expected_chain_saturation_hue_temperature.png")
    }

    @Test
    fun `generate chain grayscale-median-vignette golden`() {
        createTestImage().applyFilters {
            grayscale()
            medianBlur(1)
            vignette()
        }.savePng("expected_chain_grayscale_median_vignette.png")
    }
}
