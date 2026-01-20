package io.bluetape4k.jackson3.binary.ion

import io.bluetape4k.jackson3.binary.AbstractJacksonBinaryTest
import io.bluetape4k.jackson3.binary.IonJacksonSerializer
import io.bluetape4k.jackson3.binary.JacksonBinary
import io.bluetape4k.logging.KLogging

class IonMapperTest: AbstractJacksonBinaryTest() {

    companion object: KLogging()

    override val binaryJacksonSerializer: IonJacksonSerializer = JacksonBinary.ION.defaultSerializer

}
