package io.bluetape4k.images.filters

import com.sksamuel.scrimage.Position
import io.bluetape4k.images.coroutines.SuspendJpegWriter
import io.bluetape4k.images.suspendBytes
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.awt.Color

class CaptionFilterSupportTest: AbstractFilterTest() {

    companion object: KLoggingChannel()

    @Test
    fun `기본 설정으로 캡션 필터를 생성한다`() = runSuspendIO {
        val origin = loadResourceImage("debop.jpg")
        val filter = captionFilterOf(text = "Powered by bluetape4k")

        val captioned = origin.filter(filter)
        val bytes = captioned.suspendBytes(SuspendJpegWriter.Default)
        bytes.shouldNotBeEmpty()
    }

    @Test
    fun `위치를 지정하여 캡션 필터를 생성한다`() = runSuspendIO {
        val origin = loadResourceImage("debop.jpg")
        val filter = captionFilterOf(
            text = "bluetape4k",
            position = Position.TopRight,
            textAlpha = 0.8,
            color = Color.YELLOW
        )

        val captioned = origin.filter(filter)
        val bytes = captioned.suspendBytes(SuspendJpegWriter.Default)
        bytes.shouldNotBeEmpty()
    }

    @Test
    fun `전체 너비 캡션 필터를 생성한다`() = runSuspendIO {
        val origin = loadResourceImage("debop.jpg")
        val filter = captionFilterOf(
            text = "Full width caption",
            fullWidth = true,
            captionAlpha = 0.5
        )

        val captioned = origin.filter(filter)
        val bytes = captioned.suspendBytes(SuspendJpegWriter.Default)
        bytes.shouldNotBeEmpty()
    }
}
