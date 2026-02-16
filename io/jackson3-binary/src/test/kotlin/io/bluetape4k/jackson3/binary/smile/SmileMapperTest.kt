package io.bluetape4k.jackson3.binary.smile

import io.bluetape4k.jackson3.binary.AbstractJacksonBinaryTest
import io.bluetape4k.jackson3.binary.JacksonBinary
import io.bluetape4k.jackson3.binary.SmileJacksonSerializer
import io.bluetape4k.logging.KLogging

/**
 * Smile 바이너리 포맷의 직렬화/역직렬화를 검증하는 테스트 클래스입니다.
 */
class SmileMapperTest: AbstractJacksonBinaryTest() {

    companion object: KLogging()

    override val binaryJacksonSerializer: SmileJacksonSerializer = JacksonBinary.Smile.defaultSerializer

}
