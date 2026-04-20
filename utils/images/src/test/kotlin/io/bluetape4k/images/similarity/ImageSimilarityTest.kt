package io.bluetape4k.images.similarity

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.coroutines.SuspendJpegWriter
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.images.suspendBytes
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.Test
import java.awt.Color

class ImageSimilarityTest: AbstractImageTest() {

    companion object: KLoggingChannel() {
        private const val HOMER_JPG = "images/homer.jpg"
        private const val LABOR_JPG = "images/labor.jpg"
    }

    private fun loadImage(path: String): ImmutableImage =
        immutableImageOf(Resourcex.getInputStream(path)!!)

    @Test
    fun `identical images have zero delta and perfect scores`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(HOMER_JPG)

        a.pixelAvgDeltaTo(b) shouldBeEqualTo 0.0
        a.pixelMaxDeltaTo(b) shouldBeEqualTo 0
        a.mseTo(b) shouldBeEqualTo 0.0
        a.psnrTo(b) shouldBeEqualTo Double.POSITIVE_INFINITY
        a.ssimTo(b) shouldBeEqualTo 1.0
        a.phashDistanceTo(b) shouldBeEqualTo 0
    }

    @Test
    fun `jpeg round-trip produces small delta but high similarity`() = runSuspendIO {
        val a = loadImage(HOMER_JPG)
        val roundTripped = immutableImageOf(a.suspendBytes(SuspendJpegWriter.Default))

        val avgDelta = a.pixelAvgDeltaTo(roundTripped)
        val maxDelta = a.pixelMaxDeltaTo(roundTripped)
        val psnr = a.psnrTo(roundTripped)
        val ssim = a.ssimTo(roundTripped)
        val phashDistance = a.phashDistanceTo(roundTripped)

        log.debug(
            "jpeg round-trip: avg=$avgDelta, max=$maxDelta, psnr=$psnr, ssim=$ssim, phashDistance=$phashDistance"
        )

        avgDelta shouldBeLessThan 5.0
        maxDelta shouldBeLessThan 64
        psnr shouldBeGreaterThan 30.0
        ssim shouldBeGreaterThan 0.95
        phashDistance shouldBeLessThan 5
    }

    @Test
    fun `different images produce large delta and low similarity`() {
        val homer = loadImage(HOMER_JPG)
        val labor = loadImage(LABOR_JPG).scaleTo(homer.width, homer.height)

        val avgDelta = homer.pixelAvgDeltaTo(labor)
        val psnr = homer.psnrTo(labor)
        val ssim = homer.ssimTo(labor)
        val phashDistance = homer.phashDistanceTo(labor)

        log.debug(
            "different images: avg=$avgDelta, psnr=$psnr, ssim=$ssim, phashDistance=$phashDistance"
        )

        avgDelta shouldBeGreaterThan 10.0
        psnr shouldBeLessThan 30.0
        ssim shouldBeLessThan 0.9
        phashDistance shouldBeGreaterThan 5
    }

    @Test
    fun `scaled version keeps phash distance small`() {
        val original = loadImage(HOMER_JPG)
        val halfSize = original.scaleTo(original.width / 2, original.height / 2)

        val distance = hammingDistance(original.phash(), halfSize.phash())
        log.debug("half-scale phash distance: $distance")

        distance shouldBeLessThan 10
    }

    @Test
    fun `brightness shift keeps pHash similar`() {
        val original = loadImage(HOMER_JPG)
        val brighter = original.map { pixel ->
            val r = (pixel.red() + 20).coerceIn(0, 255)
            val g = (pixel.green() + 20).coerceIn(0, 255)
            val b = (pixel.blue() + 20).coerceIn(0, 255)
            Color(r, g, b, pixel.alpha())
        }

        val phashDistance = original.phashDistanceTo(brighter)
        val ssim = original.ssimTo(brighter)
        log.debug("brightness +20: phashDistance=$phashDistance, ssim=$ssim")

        phashDistance shouldBeLessThan 10
        ssim shouldBeGreaterThan 0.8
    }

    @Test
    fun `hammingDistance returns correct bit difference`() {
        hammingDistance(0L, 0L) shouldBeEqualTo 0
        hammingDistance(0L, 1L) shouldBeEqualTo 1
        hammingDistance(0L, 0xFFL) shouldBeEqualTo 8
        hammingDistance(0b1010L, 0b0101L) shouldBeEqualTo 4
        hammingDistance(-1L, 0L) shouldBeEqualTo 64
    }

    @Test
    fun `phash returns stable value for same input`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(HOMER_JPG)

        a.phash() shouldBeEqualTo b.phash()
    }

    @Test
    fun `ssim is bounded between -1 and 1`() {
        val a = loadImage(HOMER_JPG)
        val b = loadImage(LABOR_JPG).scaleTo(a.width, a.height)

        a.ssimTo(b) shouldBeInRange -1.0..1.0
        a.ssimTo(a) shouldBeInRange -1.0..1.0
    }
}
