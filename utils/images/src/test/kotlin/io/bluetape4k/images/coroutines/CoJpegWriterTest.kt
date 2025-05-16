package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.logging.coroutines.KLoggingChannel

class CoJpegWriterTest: AbstractCoImageWriterTest() {

    companion object: KLoggingChannel()

    override val writer: CoImageWriter = CoJpegWriter.Default
    override val imageFormat: Format = Format.JPEG
}
