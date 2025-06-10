package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.logging.coroutines.KLoggingChannel

class SuspendJpegWriterTest: AbstractSuspendImageWriterTest() {

    companion object: KLoggingChannel()

    override val writer: SuspendImageWriter = SuspendJpegWriter.Default
    override val imageFormat: Format = Format.JPEG
}
