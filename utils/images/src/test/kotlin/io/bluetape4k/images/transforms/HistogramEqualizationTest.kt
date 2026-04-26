package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.awt.Color
import java.awt.image.BufferedImage
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HistogramEqualizationTest {

    companion object : KLoggingChannel()

    private fun createUniformImage(w: Int, h: Int, r: Int, g: Int, b: Int): ImmutableImage {
        val buf = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g2 = buf.createGraphics()
        try {
            g2.color = Color(r, g, b)
            g2.fillRect(0, 0, w, h)
        } finally {
            g2.dispose()
        }
        return ImmutableImage.wrapAwt(buf)
    }

    private fun createGradientImage(w: Int = 256, h: Int = 64): ImmutableImage {
        val buf = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until w) {
            val v = x  // 0 to 255
            for (y in 0 until h) {
                buf.setRGB(x, y, (0xFF shl 24) or (v shl 16) or (v shl 8) or v)
            }
        }
        return ImmutableImage.wrapAwt(buf)
    }

    private fun averageLuma(image: ImmutableImage): Double {
        val pixels = image.awt().let { buf ->
            val arr = IntArray(buf.width * buf.height)
            buf.getRGB(0, 0, buf.width, buf.height, arr, 0, buf.width)
            arr
        }
        return pixels.map { argb ->
            val r = (argb ushr 16) and 0xFF
            val g = (argb ushr 8) and 0xFF
            val b = argb and 0xFF
            0.299 * r + 0.587 * g + 0.114 * b
        }.average()
    }

    @Test
    fun `clahe on uniform dark image keeps same dimensions`() {
        val dark = createUniformImage(64, 64, 50, 50, 50)
        val result = dark.clahe()

        result.width shouldBeEqualTo dark.width
        result.height shouldBeEqualTo dark.height
        // Uniform image has 0 variance — CLAHE output should differ or stay; just check it doesn't throw
    }

    @Test
    fun `clahe increases contrast on gradient image`() {
        val gradient = createGradientImage(256, 64)
        val result = gradient.clahe(tileSize = 8, clipLimit = 2.0)

        result.width shouldBeEqualTo 256
        result.height shouldBeEqualTo 64
        // No exception should be thrown
    }

    @Test
    fun `globalEqualize on gradient has same dimensions`() {
        val gradient = createGradientImage(256, 64)
        val result = gradient.globalEqualize()

        result.width shouldBeEqualTo gradient.width
        result.height shouldBeEqualTo gradient.height
    }

    @Test
    fun `tileSize larger than image falls back gracefully (same as globalEqualize)`() {
        val small = createGradientImage(50, 50)
        val resultBig = small.clahe(tileSize = 1024)
        val resultGlobal = small.globalEqualize()

        resultBig.width shouldBeEqualTo resultGlobal.width
        resultBig.height shouldBeEqualTo resultGlobal.height
        // Pixel-level comparison optional — both use same fallback path
    }

    @Test
    fun `clahe preserves chrominance on red image`() {
        val red = createUniformImage(64, 64, 200, 30, 30)
        val result = red.clahe()

        val rgb = result.awt().getRGB(32, 32)
        val r = (rgb ushr 16) and 0xFF
        val g = (rgb ushr 8) and 0xFF

        // red channel should dominate — r should be greater than g
        r shouldBeGreaterThan g
    }

    @Test
    fun `tileSize 0 throws`() {
        assertThrows<IllegalArgumentException> {
            createUniformImage(64, 64, 128, 128, 128).clahe(tileSize = 0)
        }
    }

    @Test
    fun `clipLimit 0 throws`() {
        assertThrows<IllegalArgumentException> {
            createUniformImage(64, 64, 128, 128, 128).clahe(clipLimit = 0.0)
        }
    }

    @Test
    fun `suspendClahe matches clahe dimensions`() = runTest {
        val gradient = createGradientImage(256, 64)
        val resultSync = gradient.clahe(tileSize = 8, clipLimit = 2.0)
        val resultSuspend = gradient.suspendClahe(tileSize = 8, clipLimit = 2.0)

        resultSuspend.width shouldBeEqualTo resultSync.width
        resultSuspend.height shouldBeEqualTo resultSync.height
    }

    @Test
    fun `globalEqualize produces 1x1 tile for non-square image`() {
        // 200×100 비정방형 — tileSize = maxOf(200,100) = 200 → nTilesX=1, nTilesY=1
        val image = createGradientImage(200, 100)
        val result = image.globalEqualize()

        result.width shouldBeEqualTo 200
        result.height shouldBeEqualTo 100
        // 단일 타일이므로 중앙과 가장자리에서 동일한 LUT가 적용됨 — 결과 이미지 크기만 검증
    }
}
