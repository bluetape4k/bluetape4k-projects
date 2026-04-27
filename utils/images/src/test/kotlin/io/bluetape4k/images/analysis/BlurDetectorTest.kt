package io.bluetape4k.images.analysis

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Resourcex
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class BlurDetectorTest {

    companion object: KLoggingChannel() {
        private const val HOMER_JPG = "images/homer.jpg"
        private const val LANDSCAPE_JPG = "images/landscape.jpg"

        private fun loadImage(path: String): ImmutableImage =
            ImmutableImage.loader().fromStream(Resourcex.getInputStream(path)!!)

        /** 단색 (완전 흐린) 이미지를 생성한다. Laplacian variance ≈ 0. */
        fun uniformImage(w: Int = 50, h: Int = 50): ImmutableImage {
            val buf = BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR)
            val gfx = buf.createGraphics()
            gfx.color = java.awt.Color(128, 128, 128)
            gfx.fillRect(0, 0, w, h)
            gfx.dispose()
            val baos = ByteArrayOutputStream()
            ImageIO.write(buf, "jpg", baos)
            return ImmutableImage.loader().fromBytes(baos.toByteArray())
        }

        /** 수직 줄무늬 (엣지 풍부) 이미지를 생성한다. Laplacian variance 높음. */
        fun stripedImage(w: Int = 50, h: Int = 50): ImmutableImage {
            val buf = BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR)
            for (x in 0 until w) {
                val c = if (x % 2 == 0) java.awt.Color.BLACK else java.awt.Color.WHITE
                for (y in 0 until h) {
                    buf.setRGB(x, y, c.rgb)
                }
            }
            val baos = ByteArrayOutputStream()
            ImageIO.write(buf, "jpg", baos)
            return ImmutableImage.loader().fromBytes(baos.toByteArray())
        }
    }

    @Test
    fun `homer image returns positive blur score`() {
        val image = loadImage(HOMER_JPG)
        val result = image.blurScore()
        log.debug { "homer blurScore: score=${result.score}, isBlurry=${result.isBlurry}" }
        result.score shouldBeGreaterThan 0.0
    }

    @Test
    fun `landscape image returns positive blur score`() {
        val image = loadImage(LANDSCAPE_JPG)
        val result = image.blurScore()
        log.debug { "landscape blurScore: score=${result.score}, isBlurry=${result.isBlurry}" }
        result.score shouldBeGreaterThan 0.0
    }

    @Test
    fun `uniform solid image has very low blur score`() {
        val image = uniformImage()
        val result = image.blurScore(threshold = 100.0)
        log.debug { "uniform blurScore: ${result.score}" }
        // 단색 이미지는 Laplacian variance가 매우 낮다 (JPEG 인코딩으로 인한 약간의 noise 허용)
        result.score shouldBeLessOrEqualTo 200.0  // JPEG noise 허용
        result.isBlurry.shouldBeTrue()
    }

    @Test
    fun `striped image has higher score than uniform image`() {
        val uniform = uniformImage()
        val striped = stripedImage()
        val uniformScore = computeLaplacianVariance(uniform)
        val stripedScore = computeLaplacianVariance(striped)
        log.debug { "uniform=$uniformScore, striped=$stripedScore" }
        stripedScore shouldBeGreaterThan uniformScore
    }

    @Test
    fun `isBlurry returns correct value based on threshold`() {
        val image = loadImage(HOMER_JPG)
        val score = image.blurScore()
        // 매우 낮은 threshold → isBlurry=false (선명)
        val veryLow = image.blurScore(threshold = 0.0)
        veryLow.isBlurry.shouldBeFalse()
        // 매우 높은 threshold → isBlurry=true
        val veryHigh = image.blurScore(threshold = Double.MAX_VALUE)
        veryHigh.isBlurry.shouldBeTrue()
        log.debug { "homer score=${score.score}, veryLow.isBlurry=${veryLow.isBlurry}, veryHigh.isBlurry=${veryHigh.isBlurry}" }
    }

    @Test
    fun `isBlurry extension matches blurScore`() {
        val image = loadImage(HOMER_JPG)
        val threshold = 50.0
        val fromScore = image.blurScore(threshold).isBlurry
        val fromExt = image.isBlurry(threshold)
        log.debug { "isBlurry match: $fromScore == $fromExt" }
        (fromScore == fromExt).shouldBeTrue()
    }

    @Test
    fun `image smaller than 3x3 throws`() {
        val tiny = ImmutableImage.create(2, 2)
        invoking {
            tiny.blurScore()
        } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `exactly 3x3 image does not throw`() {
        val small = ImmutableImage.create(3, 3)
        val result = small.blurScore()
        log.debug { "3x3 blurScore: ${result.score}" }
        result.score shouldBeGreaterOrEqualTo 0.0
    }

    @Test
    fun `BlurScore data class properties are correct`() {
        val score = BlurScore(score = 75.5, threshold = 100.0)
        score.isBlurry.shouldBeTrue()

        val sharp = BlurScore(score = 200.0, threshold = 100.0)
        sharp.isBlurry.shouldBeFalse()
    }

    @Test
    fun `threshold boundary score equals threshold is not blurry`() {
        val score = BlurScore(score = 100.0, threshold = 100.0)
        // score < threshold → isBlurry; score == threshold → NOT blurry
        score.isBlurry.shouldBeFalse()
    }

    @Test
    fun `internal computeLaplacianVariance returns non-negative value`() {
        val image = loadImage(HOMER_JPG)
        val variance = computeLaplacianVariance(image)
        log.debug { "homer laplacian variance: $variance" }
        variance shouldBeGreaterOrEqualTo 0.0
    }
}

