package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.logging.KLogging

class CoGifWriterTest: AbstractCoImageWriterTest() {

    companion object: KLogging()

    override val writer: CoImageWriter = CoGifWriter.Default
    override val imageFormat: Format = Format.GIF
}
