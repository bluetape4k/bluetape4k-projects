package io.bluetape4k.jackson.binary.ion

import io.bluetape4k.jackson.binary.AbstractJacksonBinaryTest
import io.bluetape4k.jackson.binary.IonJacksonSerializer
import io.bluetape4k.jackson.binary.JacksonBinary
import io.bluetape4k.logging.KLogging

/**
 * ION 바이너리 포맷의 직렬화/역직렬화를 검증하는 테스트 클래스입니다.
 */
class IonMapperTest: AbstractJacksonBinaryTest() {

    companion object: KLogging()

    override val binaryJacksonSerializer: IonJacksonSerializer = JacksonBinary.ION.defaultSerializer

}
