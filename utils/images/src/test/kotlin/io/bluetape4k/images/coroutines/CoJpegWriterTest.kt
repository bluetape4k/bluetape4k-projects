package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.logging.KLogging

class CoJpegWriterTest: AbstractCoImageWriterTest() {

    companion object: KLogging()

    override val writer: CoImageWriter = CoJpegWriter.Default
    override val imageFormat: Format = Format.JPEG
}
