package io.bluetape4k.jackson3.binary.cbor

import io.bluetape4k.jackson3.binary.AbstractJacksonBinaryTest
import io.bluetape4k.jackson3.binary.CborJacksonSerializer
import io.bluetape4k.jackson3.binary.JacksonBinary
import io.bluetape4k.logging.KLogging

/**
 * CBOR 바이너리 포맷의 직렬화/역직렬화를 검증하는 테스트 클래스입니다.
 */
class CborMapperTest: AbstractJacksonBinaryTest() {

    companion object: KLogging()

    override val binaryJacksonSerializer: CborJacksonSerializer = JacksonBinary.CBOR.defaultSerializer

}
