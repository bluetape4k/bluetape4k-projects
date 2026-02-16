package io.bluetape4k.jackson.text

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

/**
 * Jackson 텍스트 포맷(CSV, Properties, TOML, YAML) 테스트의 공통 설정을 제공하는 추상 클래스입니다.
 */
abstract class AbstractJacksonTextTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker
    }

}
