package io.bluetape4k.jackson.binary.smile

import io.bluetape4k.jackson.JsonSerializer
import io.bluetape4k.jackson.binary.AbstractJacksonBinaryTest
import io.bluetape4k.jackson.binary.JacksonBinary
import io.bluetape4k.logging.KLogging

class SmileMapperTest: AbstractJacksonBinaryTest() {

    companion object: KLogging()

    override val binaryJsonSerializer: JsonSerializer = JacksonBinary.Smile.defaultJsonSerializer

}
