package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.Random
import kotlin.math.abs
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MedianBlurFilterTest: AbstractFilterTest() {

    companion object: KLoggingChannel()

    /**
     * 결정론적 컬러 타일 이미지(width×height)를 생성합니다.
     * 4×4 타일로 구성되며, 각 타일은 고정된 색상 목록에서 순환 배정됩니다.
     */
    private fun createColoredImage(width: Int = 256, height: Int = 256): ImmutableImage {
        val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = buffered.createGraphics()
        try {
            val colors = listOf(
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.PINK,
            )
            var idx = 0
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    g.color = colors[idx % colors.size]
                    idx++
                    g.fillRect(col * (width / 4), row * (height / 4), width / 4, height / 4)
                }
            }
        } finally {
            g.dispose()
        }
        return ImmutableImage.fromAwt(buffered)
    }

    /**
     * 결정론적으로 salt-and-pepper 노이즈를 일정 비율로 추가한 복사본을 반환합니다.
     */
    private fun addSaltPepperNoise(image: ImmutableImage, ratio: Double = 0.05): ImmutableImage {
        val copy = image.copy()
        val awt = copy.awt()
        val random = Random(12345)
        val total = awt.width * awt.height
        val noiseCount = (total * ratio).toInt()
        repeat(noiseCount) {
            val x = random.nextInt(awt.width)
            val y = random.nextInt(awt.height)
            awt.setRGB(x, y, if (random.nextBoolean()) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
        }
        return copy
    }

    /**
     * [image]가 [original] 대비 채널 차이가 [threshold] 초과인 픽셀의 개수를 셉니다.
     */
    private fun countNoisyPixels(image: ImmutableImage, original: ImmutableImage, threshold: Int = 100): Int {
        var count = 0
        val a = image.awt()
        val b = original.awt()
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val pa = a.getRGB(x, y)
                val pb = b.getRGB(x, y)
                val dr = abs(((pa shr 16) and 0xFF) - ((pb shr 16) and 0xFF))
                val dg = abs(((pa shr 8) and 0xFF) - ((pb shr 8) and 0xFF))
                val db = abs((pa and 0xFF) - (pb and 0xFF))
                if (dr > threshold || dg > threshold || db > threshold) count++
            }
        }
        return count
    }

    /**
     * radius가 음수이면 [IllegalArgumentException]을 던져야 합니다.
     */
    @Test
    fun `radius=-1 은 IllegalArgumentException 을 던진다`() {
        assertThrows<IllegalArgumentException> {
            medianBlurFilterOf(-1)
        }
    }

    /**
     * radius=0 이면 identity 필터로 원본과 동일해야 합니다.
     */
    @Test
    fun `radius=0 은 원본과 동일한 이미지를 반환한다`() {
        val source = createColoredImage()
        val result = source.copy().filter(medianBlurFilterOf(0))
        assertSimilarToImage(result, source, tolerance = 0)
    }

    /**
     * radius=2, REPLICATE 모드는 색상 타일 경계 부근에서 블러가 발생하므로 원본과 달라야 합니다.
     */
    @Test
    fun `radius=2 REPLICATE 는 원본과 다른 결과를 반환한다`() {
        val source = createColoredImage()
        val result = source.copy().filter(medianBlurFilterOf(2, MedianBoundaryMode.REPLICATE))
        assertNotSimilarToImage(result, source, threshold = 1)
    }

    /**
     * 중앙 픽셀(width/2, height/2)은 경계 영향을 받지 않으므로 REFLECT 와 REPLICATE 결과가 동일해야 합니다.
     */
    @Test
    fun `중앙 픽셀은 REFLECT 와 REPLICATE 결과가 동일하다`() {
        val source = createColoredImage()

        val resultReplicate = source.copy().filter(medianBlurFilterOf(2, MedianBoundaryMode.REPLICATE))
        val resultReflect = source.copy().filter(medianBlurFilterOf(2, MedianBoundaryMode.REFLECT))

        val cx = source.width / 2
        val cy = source.height / 2
        val replicateRgb = resultReplicate.awt().getRGB(cx, cy)
        val reflectRgb = resultReflect.awt().getRGB(cx, cy)

        val dr = abs(((replicateRgb shr 16) and 0xFF) - ((reflectRgb shr 16) and 0xFF))
        val dg = abs(((replicateRgb shr 8) and 0xFF) - ((reflectRgb shr 8) and 0xFF))
        val db = abs((replicateRgb and 0xFF) - (reflectRgb and 0xFF))

        dr shouldBeLessThan 1
        dg shouldBeLessThan 1
        db shouldBeLessThan 1
    }

    /**
     * salt-and-pepper 노이즈가 포함된 이미지에 median 필터를 적용하면 노이즈 픽셀 비율이 감소해야 합니다.
     */
    @Test
    fun `salt-and-pepper 노이즈가 median 필터로 제거된다`() {
        val original = createColoredImage()
        val noisy = addSaltPepperNoise(original, ratio = 0.05)

        val noiseBefore = countNoisyPixels(noisy, original, threshold = 100)
        val denoised = noisy.copy().filter(medianBlurFilterOf(2, MedianBoundaryMode.REPLICATE))
        val noiseAfter = countNoisyPixels(denoised, original, threshold = 100)

        // median 필터는 임펄스 노이즈를 효과적으로 제거해야 함
        noiseAfter shouldBeLessThan noiseBefore
    }

    /**
     * radius=2 REPLICATE 결과가 골든 이미지와 일치해야 합니다 (회귀 검증).
     */
    @Test
    fun `radius=2 REPLICATE 결과는 골든 이미지와 일치한다`() {
        val result = createColoredImage().filter(medianBlurFilterOf(2, MedianBoundaryMode.REPLICATE))
        assertSimilarToResource(result, "expected_median_2.png", tolerance = 3)
    }
}
