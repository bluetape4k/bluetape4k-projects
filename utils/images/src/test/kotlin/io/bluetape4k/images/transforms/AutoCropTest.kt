package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.awt.Color
import java.awt.image.BufferedImage

class AutoCropTest : AbstractImageTest() {

    companion object : KLoggingChannel()

    /**
     * 흰 배경 위에 지정한 색상의 사각형이 그려진 테스트용 이미지를 생성합니다.
     */
    private fun createWhitePaddedImage(
        totalW: Int = 100, totalH: Int = 100,
        rectX: Int = 10, rectY: Int = 10, rectW: Int = 80, rectH: Int = 80,
        bgColor: Color = Color.WHITE,
        fgColor: Color = Color.RED,
    ): ImmutableImage {
        val buf = BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_ARGB)
        val g = buf.createGraphics()
        try {
            g.color = bgColor
            g.fillRect(0, 0, totalW, totalH)
            g.color = fgColor
            g.fillRect(rectX, rectY, rectW, rectH)
        } finally {
            g.dispose()
        }
        return ImmutableImage.wrapAwt(buf)
    }

    @Test
    fun `autoCrop removes white padding around red rectangle`() {
        val image = createWhitePaddedImage(100, 100, 10, 10, 80, 80)
        val result = image.autoCrop(tolerance = 0, backgroundColor = Color.WHITE)

        result.width shouldBeGreaterOrEqualTo 78
        result.width shouldBeLessThan 100
        result.height shouldBeGreaterOrEqualTo 78
        result.height shouldBeLessThan 100
    }

    @Test
    fun `explicit backgroundColor matches corner auto-detection`() {
        val image = createWhitePaddedImage()
        val result1 = image.autoCrop(tolerance = 0, backgroundColor = Color.WHITE)
        val result2 = image.autoCrop(tolerance = 0, backgroundColor = null)

        result1.width shouldBeEqualTo result2.width
        result1.height shouldBeEqualTo result2.height
    }

    @Test
    fun `padding adds border to cropped result`() {
        val image = createWhitePaddedImage(100, 100, 10, 10, 80, 80)
        val noPad = image.autoCrop(tolerance = 0, backgroundColor = Color.WHITE)
        val withPad = image.autoCrop(tolerance = 0, padding = 5, backgroundColor = Color.WHITE)

        // padding=5 adds up to 10 pixels per axis (clamped to original bounds)
        withPad.width shouldBeGreaterOrEqualTo (noPad.width + 8)
    }

    @Test
    fun `solid color image returns original dimensions (silent fallback)`() {
        val buf = BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB)
        val g = buf.createGraphics()
        try {
            g.color = Color.WHITE
            g.fillRect(0, 0, 50, 50)
        } finally {
            g.dispose()
        }
        val solid = ImmutableImage.wrapAwt(buf)
        val result = solid.autoCrop(tolerance = 0, backgroundColor = Color.WHITE)

        result.width shouldBeEqualTo solid.width
        result.height shouldBeEqualTo solid.height
    }

    @Test
    fun `tolerance zero vs high tolerance`() {
        // 이미지: 거의-흰색(245,245,245) 배경 위에 빨간 사각형
        val buf = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
        val g = buf.createGraphics()
        try {
            g.color = Color(245, 245, 245)  // near-white, not pure white
            g.fillRect(0, 0, 100, 100)
            g.color = Color.RED
            g.fillRect(20, 20, 60, 60)
        } finally {
            g.dispose()
        }
        val image = ImmutableImage.wrapAwt(buf)

        // tolerance=0 with pure white background — near-white is not removed
        val nocrop = image.autoCrop(tolerance = 0, backgroundColor = Color.WHITE)
        nocrop.width shouldBeEqualTo 100

        // tolerance=20 matches near-white → crop occurs
        val cropped = image.autoCrop(tolerance = 20, backgroundColor = Color.WHITE)
        cropped.width shouldBeLessThan 100
    }

    @Test
    fun `landscape jpg with tolerance=0 returns unchanged dimensions`() {
        val image = immutableImageOf(getImage(LANDSCAPE_JPG))
        val result = image.autoCrop(tolerance = 0)

        result.width shouldBeEqualTo image.width
        result.height shouldBeEqualTo image.height
    }

    @Test
    fun `suspendAutoCrop produces same result as autoCrop`() = runTest {
        val image = createWhitePaddedImage(100, 100, 10, 10, 80, 80)
        val sync = image.autoCrop(tolerance = 0, backgroundColor = Color.WHITE)
        val suspended = image.suspendAutoCrop(tolerance = 0, backgroundColor = Color.WHITE)

        suspended.width shouldBeEqualTo sync.width
        suspended.height shouldBeEqualTo sync.height
    }

    @Test
    fun `negative tolerance throws`() {
        val image = createWhitePaddedImage()
        assertThrows<IllegalArgumentException> {
            image.autoCrop(tolerance = -1)
        }
    }

    @Test
    fun `negative padding throws`() {
        val image = createWhitePaddedImage()
        assertThrows<IllegalArgumentException> {
            image.autoCrop(padding = -1)
        }
    }
}
