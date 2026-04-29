package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class SmartCropTest : AbstractImageTest() {

    companion object : KLoggingChannel()

    /**
     * 왼쪽 절반은 체커보드(흑백 10px 격자), 오른쪽 절반은 단색 라이트그레이인 이미지를 생성합니다.
     *
     * SmartCrop 이 체커보드(엣지 에너지 높음) 쪽을 선택하는지 검증하는 데 사용합니다.
     */
    private fun createCheckerboardLeftSolidRight(w: Int = 200, h: Int = 100): ImmutableImage {
        val buf = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = buf.createGraphics()
        try {
            // 왼쪽 절반: 10px 체커보드 (흑백 교차)
            for (y in 0 until h) {
                for (x in 0 until w / 2) {
                    g.color = if ((x / 10 + y / 10) % 2 == 0) Color.BLACK else Color.WHITE
                    g.fillRect(x, y, 1, 1)
                }
            }
            // 오른쪽 절반: 단색 라이트그레이
            g.color = Color.LIGHT_GRAY
            g.fillRect(w / 2, 0, w / 2, h)
        } finally {
            g.dispose()
        }
        return ImmutableImage.wrapAwt(buf)
    }

    @Test
    fun `smartCrop SQUARE selects content-rich region`() {
        val image = createCheckerboardLeftSolidRight(200, 100)

        val result = image.smartCrop(AspectRatio.SQUARE)

        // 결과가 대략 정사각형인지 확인 (±25% 허용)
        val ratio = result.width.toDouble() / result.height
        ratio shouldBeInRange (0.8..1.25)
        result.width shouldBeGreaterThan 0
        result.height shouldBeGreaterThan 0
    }

    @Test
    fun `smartCrop WIDESCREEN aspect ratio is correct`() {
        val image = immutableImageOf(Resourcex.getInputStream(LANDSCAPE_JPG)!!)

        val result = image.smartCrop(AspectRatio.WIDESCREEN)

        // 16:9 ≈ 1.778, 허용 범위 1.6..1.9
        val ratio = result.width.toDouble() / result.height
        ratio shouldBeInRange (1.6..1.9)
    }

    @Test
    fun `smartCrop PORTRAIT aspect ratio`() {
        val image = immutableImageOf(Resourcex.getInputStream(LANDSCAPE_JPG)!!)

        val result = image.smartCrop(AspectRatio.PORTRAIT)

        // 9:16 세로형이므로 높이/너비 ≈ 1.778, 허용 범위 1.6..1.9
        val ratio = result.height.toDouble() / result.width
        ratio shouldBeInRange (1.6..1.9)
    }

    @Test
    fun `smartCropTo returns exact dimensions`() {
        val image = immutableImageOf(Resourcex.getInputStream(LANDSCAPE_JPG)!!)

        val result = image.smartCropTo(320, 240)

        result.width shouldBeEqualTo 320
        result.height shouldBeEqualTo 240
    }

    @Test
    fun `smartCrop on solid white image does not throw`() {
        val buf = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB)
        val g = buf.createGraphics()
        try {
            g.color = Color.WHITE
            g.fillRect(0, 0, 100, 100)
        } finally {
            g.dispose()
        }
        val white = ImmutableImage.wrapAwt(buf)

        val result = white.smartCrop(AspectRatio.SQUARE)

        result.width shouldBeGreaterThan 0
        result.height shouldBeGreaterThan 0
    }

    @Test
    fun `invalid AspectRatio throws`() {
        val throwOnZeroWidth = { AspectRatio(0, 1) }
        val throwOnNegativeHeight = { AspectRatio(1, -1) }

        throwOnZeroWidth shouldThrow IllegalArgumentException::class
        throwOnNegativeHeight shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `suspendSmartCrop matches smartCrop dimensions`() = runTest {
        val image = immutableImageOf(Resourcex.getInputStream(LANDSCAPE_JPG)!!)

        val syncResult = image.smartCrop(AspectRatio.WIDESCREEN)
        val suspendResult = image.suspendSmartCrop(AspectRatio.WIDESCREEN)

        suspendResult.width shouldBeEqualTo syncResult.width
        suspendResult.height shouldBeEqualTo syncResult.height
    }
}
