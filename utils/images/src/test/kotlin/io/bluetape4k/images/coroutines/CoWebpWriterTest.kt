package io.bluetape4k.images.coroutines

import com.sksamuel.scrimage.format.Format
import io.bluetape4k.logging.KLogging

class CoWebpWriterTest: AbstractCoImageWriterTest() {

    companion object: KLogging()

    override val writer: CoImageWriter = CoWebpWriter.Default
    override val imageFormat: Format = Format.WEBP
}
