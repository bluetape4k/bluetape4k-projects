package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.logging.KLogging

class CoPngWriterTest: AbstractCoImageWriterTest() {

    companion object: KLogging()

    override val writer: CoImageWriter = CoPngWriter.MaxCompression
    override val imageFormat: Format = Format.PNG
}
