package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
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

    /**
     * 이미지를 JPEG 90% 품질로 재저장한 후 [ImmutableImage]로 반환합니다.
     */
    private fun ImmutableImage.toJpeg90(): ImmutableImage =
        immutableImageOf(bytes(JpegWriter(90, false)))

    @Test
    fun `동일 이미지의 blockMeanSimilarityTo는 1에 가까워야 한다`() {
        val homer = loadImage(HOMER_JPG)
        val score = homer.blockMeanSimilarityTo(homer)

        log.debug("identical similarity: $score")
        score shouldBeGreaterThan 0.99
    }

    @Test
    fun `JPEG 90 percent 재저장 이미지의 blockMeanSimilarityTo는 0_9 초과여야 한다`() {
        val original = loadImage(HOMER_JPG)
        val reencoded = original.toJpeg90()
        val score = original.blockMeanSimilarityTo(reencoded)

        log.debug("jpeg90 similarity: $score")
        score shouldBeGreaterThan 0.9
    }

    @Test
    fun `다른 이미지의 blockMeanSimilarityTo는 0_5 미만이어야 한다`() {
        val homer = loadImage(HOMER_JPG)
        val landscape = loadImage(LANDSCAPE_JPG)
        val score = homer.blockMeanSimilarityTo(landscape)

        log.debug("different images similarity: $score")
        score shouldBeLessThan 0.5
    }

    @Test
    fun `90도 회전 이미지는 blockMeanSimilarityTo 낮고 bestRotationSimilarityTo 높아야 한다`() {
        val homer = loadImage(HOMER_JPG)
        val rotated = homer.rotateLeft()

        val straightScore = homer.blockMeanSimilarityTo(rotated)
        val bestScore = homer.bestRotationSimilarityTo(rotated)

        log.debug("90 rotated: straightScore=$straightScore, bestScore=$bestScore")
        straightScore shouldBeLessThan 0.9
        bestScore shouldBeGreaterThan 0.9
    }

    @Test
    fun `blockMeanDescriptor 길이는 gridRows x gridCols 이어야 한다`() {
        val homer = loadImage(HOMER_JPG)
        val desc = homer.blockMeanDescriptor(gridRows = 8, gridCols = 8)

        desc.size shouldBeEqualTo 64
    }

    @Test
    fun `gridRows가 1 미만이면 IllegalArgumentException이 발생해야 한다`() {
        val homer = loadImage(HOMER_JPG)

        try {
            homer.blockMeanDescriptor(gridRows = 0, gridCols = 8)
            throw AssertionError("IllegalArgumentException이 발생해야 합니다")
        } catch (e: IllegalArgumentException) {
            log.debug("Expected exception: ${e.message}")
        }
    }

    @Test
    fun `gridCols가 1 미만이면 IllegalArgumentException이 발생해야 한다`() {
        val homer = loadImage(HOMER_JPG)

        try {
            homer.blockMeanDescriptor(gridRows = 8, gridCols = 0)
            throw AssertionError("IllegalArgumentException이 발생해야 합니다")
        } catch (e: IllegalArgumentException) {
            log.debug("Expected exception: ${e.message}")
        }
    }
}
