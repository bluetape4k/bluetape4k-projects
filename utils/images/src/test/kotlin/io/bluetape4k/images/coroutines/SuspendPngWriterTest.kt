package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.logging.coroutines.KLoggingChannel

class SuspendPngWriterTest: AbstractSuspendImageWriterTest() {

    companion object: KLoggingChannel()

    override val writer: SuspendImageWriter = SuspendPngWriter.MaxCompression
    override val imageFormat: Format = Format.PNG
}
