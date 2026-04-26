package io.bluetape4k.images.transforms

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.AbstractImageTest
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInRange
import org.junit.jupiter.api.Test
import java.awt.Color

class RotationTest : AbstractImageTest() {

    companion object : KLoggingChannel()

    private fun loadCafeImage(): ImmutableImage = immutableImageOf(getImage(CAFE_JPG))

    @Test
    fun `0 degree rotation returns same dimensions`() {
        val image = loadCafeImage()
        val result = image.rotateDegrees(0.0)

        result.width shouldBeEqualTo image.width
        result.height shouldBeEqualTo image.height
    }

    @Test
    fun `90 degree rotation equals rotateRight`() {
        val image = loadCafeImage()
        val rotated = image.rotateDegrees(90.0)
        val expected = image.rotateRight()

        rotated.width shouldBeEqualTo expected.width
        rotated.height shouldBeEqualTo expected.height
    }

    @Test
    fun `-90 degree equals rotateLeft`() {
        val image = loadCafeImage()
        val rotated = image.rotateDegrees(-90.0)
        val expected = image.rotateLeft()

        rotated.width shouldBeEqualTo expected.width
        rotated.height shouldBeEqualTo expected.height
    }

    @Test
    fun `15 degree rotation expands bounds`() {
        val image = loadCafeImage()
        val r = Math.toRadians(15.0)
        val expectedW = Math.ceil(image.width * Math.cos(r) + image.height * Math.sin(r)).toInt()
        val expectedH = Math.ceil(image.width * Math.sin(r) + image.height * Math.cos(r)).toInt()
        val result = image.rotateDegrees(15.0)

        result.width shouldBeInRange (expectedW - 2)..(expectedW + 2)
        result.height shouldBeInRange (expectedH - 2)..(expectedH + 2)
    }

    @Test
    fun `45 degree with white background has white corner`() {
        val image = loadCafeImage()
        val result = image.rotateDegrees(45.0, background = Color.WHITE)

        // corner pixel (0,0) should be white (red channel > 200)
        val cornerRgb = result.awt().getRGB(0, 0)
        val red = (cornerRgb ushr 16) and 0xFF
        red shouldBeGreaterThan 200
    }

    @Test
    fun `double flipHorizontal returns original dimensions`() {
        val image = loadCafeImage()
        val result = image.flipHorizontal().flipHorizontal()

        result.width shouldBeEqualTo image.width
        result.height shouldBeEqualTo image.height
    }

    @Test
    fun `double flipVertical returns original dimensions`() {
        val image = loadCafeImage()
        val result = image.flipVertical().flipVertical()

        result.width shouldBeEqualTo image.width
        result.height shouldBeEqualTo image.height
    }

    @Test
    fun `180 degree twice returns original dimensions`() {
        val image = loadCafeImage()
        val twice = image.rotateDegrees(180.0).rotateDegrees(180.0)

        twice.width shouldBeInRange (image.width - 2)..(image.width + 2)
        twice.height shouldBeInRange (image.height - 2)..(image.height + 2)

        // center pixel check: should be close to original center pixel (tolerance 5 per channel)
        val cx = image.width / 2
        val cy = image.height / 2
        val origRgb = image.awt().getRGB(cx, cy)
        val twiceRgb = twice.awt().getRGB(cx, cy)

        val dr = kotlin.math.abs(((origRgb ushr 16) and 0xFF) - ((twiceRgb ushr 16) and 0xFF))
        val dg = kotlin.math.abs(((origRgb ushr 8) and 0xFF) - ((twiceRgb ushr 8) and 0xFF))
        val db = kotlin.math.abs((origRgb and 0xFF) - (twiceRgb and 0xFF))

        dr shouldBeInRange 0..5
        dg shouldBeInRange 0..5
        db shouldBeInRange 0..5
    }

    @Test
    fun `suspendRotateDegrees produces same dimensions as rotateDegrees`() = runTest {
        val image = loadCafeImage()
        val sync = image.rotateDegrees(30.0)
        val suspended = image.suspendRotateDegrees(30.0)

        suspended.width shouldBeEqualTo sync.width
        suspended.height shouldBeEqualTo sync.height
    }
}
