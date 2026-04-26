package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.filters.AbstractFilterTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage

class ImageFilterMutationTest : AbstractFilterTest() {

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
    fun `two sepia calls on same source produce identical results`() {
        val result1 = image.applyFilters { sepia() }
        val result2 = image.applyFilters { sepia() }

        // Both calls start from same immutable source — results must be identical
        assertSimilarToImage(result1, result2, tolerance = 0)
    }

    @Test
    fun `different saturation factors produce different results`() {
        val highSaturation = image.applyFilters { saturation(1.5f) }
        val lowSaturation = image.applyFilters { saturation(0.5f) }

        assertNotSimilarToImage(highSaturation, lowSaturation, threshold = 5)
    }

    @Test
    fun `source pixel unchanged after DSL apply`() {
        val beforeRgb = image.awt().getRGB(0, 0)
        image.applyFilters { sepia() }
        image.awt().getRGB(0, 0) shouldBeEqualTo beforeRgb
    }
}
