package io.bluetape4k.images.filters.dsl

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.filters.AbstractFilterTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicReference

class ImageFilterDslApplyTest : AbstractFilterTest() {

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
    fun `empty block - returns same source instance`() {
        val result = image.applyFilters { }
        // compactAndApply returns source directly when ops is empty
        result shouldBeEqualTo image
    }

    @Test
    fun `single sepia filter - result differs from original`() {
        val result = image.applyFilters { sepia() }
        assertNotSimilarToImage(result, image, threshold = 5)
    }

    @Test
    fun `mutation isolation - original pixel unchanged after applyFilters`() {
        val originalRgb = image.awt().getRGB(32, 32)
        image.applyFilters { sepia() }
        image.awt().getRGB(32, 32) shouldBeEqualTo originalRgb
    }

    @Test
    fun `suspendApplyFilters runs on DefaultDispatcher`() = runSuspendIO {
        val ref = AtomicReference<String>()
        image.suspendApplyFilters {
            pixel { img ->
                ref.set(Thread.currentThread().name)
                img
            }
        }
        ref.get() shouldContain "DefaultDispatcher"
    }

    @Test
    fun `chain order matters - sepia then invert differs from invert then sepia`() {
        val sepiaFirst = image.applyFilters {
            sepia()
            invert()
        }
        val invertFirst = image.applyFilters {
            invert()
            sepia()
        }
        assertNotSimilarToImage(sepiaFirst, invertFirst, threshold = 5)
    }
}
