package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.logging.coroutines.KLoggingChannel

class CoGifWriterTest: AbstractCoImageWriterTest() {

    companion object: KLoggingChannel()

    override val writer: CoImageWriter = CoGifWriter.Default
    override val imageFormat: Format = Format.GIF
}
