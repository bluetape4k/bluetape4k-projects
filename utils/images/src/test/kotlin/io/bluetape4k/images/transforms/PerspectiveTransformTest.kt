package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.image.BufferedImage

class PerspectiveTransformTest : AbstractImageTest() {

    companion object : KLoggingChannel()

    /**
     * 단색으로 채워진 테스트용 [ImmutableImage]를 생성합니다.
     */
    private fun createSolidImage(w: Int, h: Int, color: Color): ImmutableImage {
        val buf = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = buf.createGraphics()
        try {
            g.color = color
            g.fillRect(0, 0, w, h)
        } finally {
            g.dispose()
        }
        return ImmutableImage.wrapAwt(buf)
    }

    /** packed ARGB int 에서 빨간 채널(0..255)을 추출합니다. */
    private fun Int.redBits(): Int = (this ushr 16) and 0xFF

    /** packed ARGB int 에서 초록 채널(0..255)을 추출합니다. */
    private fun Int.greenBits(): Int = (this ushr 8) and 0xFF

    @Test
    fun `identity mapping returns same dimensions`() {
        val image = immutableImageOf(Resourcex.getInputStream(CAFE_JPG)!!)
        val w = image.width
        val h = image.height

        val srcCorners = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(w.toDouble(), 0.0),
            ImagePoint(w.toDouble(), h.toDouble()),
            ImagePoint(0.0, h.toDouble()),
        )
        val dstCorners = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(w.toDouble(), 0.0),
            ImagePoint(w.toDouble(), h.toDouble()),
            ImagePoint(0.0, h.toDouble()),
        )

        val result = image.perspectiveTransform(srcCorners, dstCorners, w, h)

        result.width shouldBeEqualTo w
        result.height shouldBeEqualTo h
    }

    @Test
    fun `outsideColor fills areas outside source region`() {
        // 50×50 소스 이미지, 파란색으로 채움
        val image = createSolidImage(50, 50, Color.BLUE)

        // sourceCorners 가 소스 이미지 경계 바깥(-10,-10)까지 확장 → 출력(0,0) 역매핑이 소스 밖
        val srcCorners = listOf(
            ImagePoint(-10.0, -10.0),
            ImagePoint(60.0, -10.0),
            ImagePoint(60.0, 60.0),
            ImagePoint(-10.0, 60.0),
        )
        val dstCorners = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(100.0, 0.0),
            ImagePoint(100.0, 100.0),
            ImagePoint(0.0, 100.0),
        )

        val result = image.perspectiveTransform(srcCorners, dstCorners, 100, 100, outsideColor = Color.RED)

        // 출력(0,0)의 역매핑은 소스(-10,-10) → 소스 범위 밖 → outsideColor(빨간색)
        val packed = result.awt().getRGB(0, 0)
        val red = packed.redBits()
        val green = packed.greenBits()

        red shouldBeGreaterThan 200
        green shouldBeLessThan 50
    }

    @Test
    fun `sourceCorners wrong size throws`() {
        val image = createSolidImage(100, 100, Color.GRAY)
        val dst4 = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(100.0, 0.0),
            ImagePoint(100.0, 100.0),
            ImagePoint(0.0, 100.0),
        )
        val tooFew = listOf(ImagePoint(0.0, 0.0))

        val block = { image.perspectiveTransform(tooFew, dst4, 100, 100) }
        block shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `outputWidth zero throws`() {
        val image = createSolidImage(100, 100, Color.GRAY)
        val src4 = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(100.0, 0.0),
            ImagePoint(100.0, 100.0),
            ImagePoint(0.0, 100.0),
        )
        val dst4 = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(100.0, 0.0),
            ImagePoint(100.0, 100.0),
            ImagePoint(0.0, 100.0),
        )

        val block = { image.perspectiveTransform(src4, dst4, 0, 100) }
        block shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `output too large throws`() {
        val image = createSolidImage(100, 100, Color.GRAY)
        val src4 = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(100.0, 0.0),
            ImagePoint(100.0, 100.0),
            ImagePoint(0.0, 100.0),
        )
        val dst4 = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(10000.0, 0.0),
            ImagePoint(10000.0, 10000.0),
            ImagePoint(0.0, 10000.0),
        )

        // 10000×10000 = 100M > 64M 한계
        val block = { image.perspectiveTransform(src4, dst4, 10000, 10000) }
        block shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `collinear source points throws`() {
        val image = createSolidImage(100, 100, Color.GRAY)

        // 소스 4개 점이 모두 동일 직선 (y=0) 위에 있음 → 호모그래피 연산 실패
        val collinearSrc = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(10.0, 0.0),
            ImagePoint(20.0, 0.0),
            ImagePoint(30.0, 0.0),
        )
        val dst4 = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(100.0, 0.0),
            ImagePoint(100.0, 100.0),
            ImagePoint(0.0, 100.0),
        )

        val block = { image.perspectiveTransform(collinearSrc, dst4, 100, 100) }
        block shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `suspendPerspectiveTransform matches perspectiveTransform dimensions`() = runTest {
        val image = immutableImageOf(Resourcex.getInputStream(CAFE_JPG)!!)
        val w = image.width
        val h = image.height

        val srcCorners = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(w.toDouble(), 0.0),
            ImagePoint(w.toDouble(), h.toDouble()),
            ImagePoint(0.0, h.toDouble()),
        )
        val dstCorners = listOf(
            ImagePoint(0.0, 0.0),
            ImagePoint(w.toDouble(), 0.0),
            ImagePoint(w.toDouble(), h.toDouble()),
            ImagePoint(0.0, h.toDouble()),
        )

        val syncResult = image.perspectiveTransform(srcCorners, dstCorners, w, h)
        val suspendResult = image.suspendPerspectiveTransform(srcCorners, dstCorners, w, h)

        suspendResult.width shouldBeEqualTo syncResult.width
        suspendResult.height shouldBeEqualTo syncResult.height
    }
}
