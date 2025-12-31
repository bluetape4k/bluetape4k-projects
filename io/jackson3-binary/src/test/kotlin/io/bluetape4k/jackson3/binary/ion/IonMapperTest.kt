package io.bluetape4k.jackson3.binary.ion

import io.bluetape4k.jackson3.binary.AbstractJacksonBinaryTest
import io.bluetape4k.jackson3.binary.JacksonBinary
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging

class IonMapperTest: AbstractJacksonBinaryTest() {

    companion object: KLogging()

    override val binaryJsonSerializer: JsonSerializer = JacksonBinary.ION.defaultJsonSerializer

}
