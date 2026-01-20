package io.bluetape4k.jackson.binary.cbor

import io.bluetape4k.jackson.binary.AbstractJacksonBinaryTest
import io.bluetape4k.jackson.binary.JacksonBinary
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging

class CborMapperTest: AbstractJacksonBinaryTest() {

    companion object: KLogging()

    override val binaryJacksonSerializer: JsonSerializer = JacksonBinary.CBOR.defaultSerializer

}
