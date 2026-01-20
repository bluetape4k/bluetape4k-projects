package io.bluetape4k.jackson.binary.ion

import io.bluetape4k.jackson.binary.AbstractJacksonBinaryTest
import io.bluetape4k.jackson.binary.JacksonBinary
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.logging.KLogging

class IonMapperTest: AbstractJacksonBinaryTest() {

    companion object: KLogging()

    override val binaryJacksonSerializer: JsonSerializer = JacksonBinary.ION.defaultSerializer

}
