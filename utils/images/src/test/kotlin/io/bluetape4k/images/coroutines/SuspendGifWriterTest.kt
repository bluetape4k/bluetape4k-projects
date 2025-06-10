package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.logging.coroutines.KLoggingChannel

class SuspendGifWriterTest: AbstractSuspendImageWriterTest() {

    companion object: KLoggingChannel()

    override val writer: SuspendImageWriter = SuspendGifWriter.Default
    override val imageFormat: Format = Format.GIF
}
