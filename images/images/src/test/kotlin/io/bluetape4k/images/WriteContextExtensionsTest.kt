package io.bluetape4k.images

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class WriteContextExtensionsTest: AbstractImageTest() {

    companion object: KLoggingChannel()

    @Test
    fun `WriteContext를 ByteArray로 변환한다`() {
        getImage(CAFE_JPG).use { input ->
            val image = ImmutableImage.loader().fromStream(input)
            val writeContext = image.forWriter(JpegWriter.Default)

            val bytes = writeContext.toByteArray()
            bytes.shouldNotBeEmpty()
            bytes.size shouldBeGreaterThan 0
        }
    }

    @Test
    fun `다른 포맷의 WriteContext를 ByteArray로 변환한다`() {
        getImage(LANDSCAPE_JPG).use { input ->
            val image = ImmutableImage.loader().fromStream(input)
            val writeContext = image.forWriter(JpegWriter(90, false))

            val bytes = writeContext.toByteArray()
            bytes.shouldNotBeEmpty()
        }
    }
}
