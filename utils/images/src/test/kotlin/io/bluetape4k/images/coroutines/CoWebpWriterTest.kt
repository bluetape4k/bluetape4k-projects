package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.logging.coroutines.KLoggingChannel

class CoWebpWriterTest: AbstractCoImageWriterTest() {

    companion object: KLoggingChannel()

    override val writer: CoImageWriter = CoWebpWriter.Default
    override val imageFormat: Format = Format.WEBP
}
