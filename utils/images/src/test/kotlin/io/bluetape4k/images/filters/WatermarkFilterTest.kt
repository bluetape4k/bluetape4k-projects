package io.bluetape4k.images.filters

import com.sksamuel.scrimage.ImmutableImage
import io.bluetape4k.images.coroutines.SuspendJpegWriter
import io.bluetape4k.images.fonts.fontOf
import io.bluetape4k.images.forSuspendWriter
import io.bluetape4k.images.immutableImageOf
import io.bluetape4k.images.suspendBytes
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.api.Test
import java.awt.Color
import kotlin.math.abs

class WatermarkFilterTest: AbstractFilterTest() {

    companion object: KLoggingChannel() {
        // JPEG 양자화 오차 + OS별 폰트 rasterizer 차이를 허용하는 픽셀 임계값.
        // RGB 채널당 평균 차이·최대 차이 상한. 수치가 초과되면 회귀로 간주.
        private const val AVG_PIXEL_DELTA_TOLERANCE = 20.0
        private const val MAX_PIXEL_DELTA_TOLERANCE = 160
    }

    // write 내용이 바뀔 시에 true로 변경한 후 테스트를 실행하면 새로운 이미지 파일이 생성됩니다.
    private val saveResult = false

    @Test
    fun `add cover watermark`() = runSuspendIO {
        val origin = loadResourceImage("debop.jpg")
        val coverWatermark = watermarkFilterOf("bluetape4k.io", type = WatermarkFilterType.COVER, alpha = 0.4)
        val marked = origin.filter(coverWatermark)

        val resultFilename = "debop_watermark_cover.jpg"
        if (saveResult) {
            marked.forSuspendWriter(SuspendJpegWriter.Default).write(resultFilename)
        }
        marked.assertSimilarToResource(resultFilename)
    }

    @Test
    fun `add stamp watermark`() = runSuspendIO {
        val origin = loadResourceImage("debop.jpg")
        val stampWatermark = watermarkFilterOf(
            "bluetape4k.io",
            font = fontOf(size = 48),
            type = WatermarkFilterType.STAMP,
            alpha = 0.4,
        )
        val marked = origin.filter(stampWatermark)

        val resultFilename = "debop_watermark_stamp.jpg"
        if (saveResult) {
            marked.forSuspendWriter(SuspendJpegWriter.Default).write(resultFilename)
        }
        marked.assertSimilarToResource(resultFilename)
    }

    @Test
    fun `add located watermark`() = runSuspendIO {
        val origin = loadResourceImage("debop.jpg")
        val font = fontOf(size = 24)
        val watermark = watermarkFilterOf(
            "created by bluetape4k.io",
            25,
            origin.height - 15,
            font,
            true,
            0.4,
            Color.WHITE
        )
        val marked = origin.filter(watermark)

        val resultFilename = "debop_watermark.jpg"
        if (saveResult) {
            marked.forSuspendWriter(SuspendJpegWriter.Default).write(resultFilename)
        }

        marked.assertSimilarToResource(resultFilename)
    }

    private suspend fun ImmutableImage.assertSimilarToResource(resultFilename: String) {
        val actualJpegBytes = suspendBytes(SuspendJpegWriter.Default)
        val actual = immutableImageOf(actualJpegBytes)
        val expected = loadResourceImage(resultFilename)

        actual.width shouldBeEqualTo expected.width
        actual.height shouldBeEqualTo expected.height

        val actualPixels = actual.pixels()
        val expectedPixels = expected.pixels()

        var totalChannelDelta = 0L
        var maxChannelDelta = 0
        for (i in actualPixels.indices) {
            val a = actualPixels[i]
            val e = expectedPixels[i]
            val dr = abs(a.red() - e.red())
            val dg = abs(a.green() - e.green())
            val db = abs(a.blue() - e.blue())
            totalChannelDelta += dr + dg + db
            val localMax = maxOf(dr, dg, db)
            if (localMax > maxChannelDelta) maxChannelDelta = localMax
        }
        val avgChannelDelta = totalChannelDelta.toDouble() / (actualPixels.size * 3)

        log.debug("[$resultFilename] avg delta=$avgChannelDelta, max delta=$maxChannelDelta")

        avgChannelDelta shouldBeLessOrEqualTo AVG_PIXEL_DELTA_TOLERANCE
        maxChannelDelta shouldBeLessOrEqualTo MAX_PIXEL_DELTA_TOLERANCE
    }
}
