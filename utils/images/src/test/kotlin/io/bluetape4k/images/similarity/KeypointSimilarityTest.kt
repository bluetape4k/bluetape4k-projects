package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test

/**
 * [blockMeanDescriptor], [blockMeanSimilarityTo], [bestRotationSimilarityTo] 테스트.
 */
class KeypointSimilarityTest : AbstractImageTest() {

    companion object : KLoggingChannel() {
        private const val HOMER_JPG = "images/homer.jpg"
        private const val LANDSCAPE_JPG = "images/landscape.jpg"
    }

    private fun loadImage(path: String): ImmutableImage =
        immutableImageOf(Resourcex.getInputStream(path)!!)

    private fun ImmutableImage.toJpeg90(): ImmutableImage =
        immutableImageOf(bytes(JpegWriter(90, false)))

    @Test
    fun `identical image block-mean similarity is near 1`() {
        val homer = loadImage(HOMER_JPG)
        val score = homer.blockMeanSimilarityTo(homer)

        log.debug("identical similarity: $score")
        score shouldBeGreaterThan 0.99
    }

    @Test
    fun `jpeg 90 percent re-encoding keeps block-mean similarity high`() {
        val original = loadImage(HOMER_JPG)
        val reencoded = original.toJpeg90()
        val score = original.blockMeanSimilarityTo(reencoded)

        log.debug("jpeg90 similarity: $score")
        score shouldBeGreaterThan 0.9
    }

    @Test
    fun `different images have low block-mean similarity`() {
        val homer = loadImage(HOMER_JPG)
        val landscape = loadImage(LANDSCAPE_JPG)
        val score = homer.blockMeanSimilarityTo(landscape)

        log.debug("different images similarity: $score")
        score shouldBeLessThan 0.5
    }

    @Test
    fun `90-degree rotation has low straight similarity but high bestRotation similarity`() {
        val homer = loadImage(HOMER_JPG)
        val rotated = homer.rotateLeft()

        val straightScore = homer.blockMeanSimilarityTo(rotated)
        val bestScore = homer.bestRotationSimilarityTo(rotated)

        log.debug("90 rotated: straightScore=$straightScore, bestScore=$bestScore")
        straightScore shouldBeLessThan 0.9
        bestScore shouldBeGreaterThan 0.9
    }

    @Test
    fun `blockMeanDescriptor length equals gridRows times gridCols`() {
        val homer = loadImage(HOMER_JPG)
        val desc = homer.blockMeanDescriptor(gridRows = 8, gridCols = 8)

        desc.size shouldBeEqualTo 64
    }

    @Test
    fun `gridRows less than 1 throws IllegalArgumentException`() {
        val homer = loadImage(HOMER_JPG)

        invoking { homer.blockMeanDescriptor(gridRows = 0, gridCols = 8) } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `gridCols less than 1 throws IllegalArgumentException`() {
        val homer = loadImage(HOMER_JPG)

        invoking { homer.blockMeanDescriptor(gridRows = 8, gridCols = 0) } shouldThrow IllegalArgumentException::class
    }
}
