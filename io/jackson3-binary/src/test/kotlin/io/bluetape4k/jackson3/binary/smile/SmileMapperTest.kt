package io.bluetape4k.jackson3.binary.smile

import io.bluetape4k.jackson3.binary.AbstractJacksonBinaryTest
import io.bluetape4k.jackson3.binary.JacksonBinary
import io.bluetape4k.jackson3.binary.SmileJacksonSerializer
import io.bluetape4k.logging.KLogging

class SmileMapperTest: AbstractJacksonBinaryTest() {

    companion object: KLogging()

    override val binaryJacksonSerializer: SmileJacksonSerializer = JacksonBinary.Smile.defaultSerializer

}
