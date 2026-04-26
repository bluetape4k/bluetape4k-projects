package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.BrightnessFilter
import com.sksamuel.scrimage.filter.ContrastFilter
import com.sksamuel.scrimage.filter.SepiaFilter
import io.bluetape4k.images.filters.AbstractFilterTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class ImageFilterPipelineTest : AbstractFilterTest() {

    companion object : KLoggingChannel()

    private lateinit var image: ImmutableImage

    @BeforeEach
    fun setUp() {
        image = createTestImage()
    }

    private fun createTestImage(): ImmutableImage {
        val buffered = BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB)
        val g = buffered.createGraphics()
        try {
            g.color = java.awt.Color.RED; g.fillRect(0, 0, 32, 32)
            g.color = java.awt.Color.GREEN; g.fillRect(32, 0, 32, 32)
            g.color = java.awt.Color.BLUE; g.fillRect(0, 32, 32, 32)
            g.color = java.awt.Color.YELLOW; g.fillRect(32, 32, 32, 32)
        } finally {
            g.dispose()
        }
        return ImmutableImage.fromAwt(buffered)
    }

    @Test
    fun `adjacent Native filters grouped by PipelineFilter produce same result as sequential apply`() {
        val viaChain = image.applyFilters {
            brightness(1.2f)
            contrast(1.1)
            sepia()
        }

        // Apply each filter sequentially to verify result equivalence
        val sequential = image
            .filter(BrightnessFilter(1.2f))
            .filter(ContrastFilter(1.1))
            .filter(SepiaFilter())

        assertSimilarToImage(viaChain, sequential, tolerance = 2)
    }

    @Test
    fun `pixel op between natives does not alter result`() {
        // native1 + pixel(identity) + native2 should equal native1 + native2
        val withIdentityPixel = image.applyFilters {
            brightness(1.2f)
            pixel { it }
            contrast(1.1)
        }

        val withoutPixel = image.applyFilters {
            brightness(1.2f)
            contrast(1.1)
        }

        assertSimilarToImage(withIdentityPixel, withoutPixel, tolerance = 2)
    }

    @Test
    fun `single Native op works correctly`() {
        val viaChain = image.applyFilters { brightness(1.5f) }
        val direct = image.filter(BrightnessFilter(1.5f))

        assertSimilarToImage(viaChain, direct, tolerance = 2)
    }
}
