package io.bluetape4k.fastjson2

import io.bluetape4k.fastjson2.AbstractFastjson2Test.Companion.faker
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

/**
 * Fastjson2 모듈 테스트를 위한 추상 기반 클래스입니다.
 *
 * 테스트에 공통으로 필요한 상수와 [faker] 인스턴스를 제공합니다.
 */
abstract class AbstractFastjson2Test {

    companion object: KLogging() {
        /** 반복 테스트 횟수 */
        const val REPEAT_SIZE = 5

        /** 테스트 데이터 생성용 Faker 인스턴스 */
        @JvmStatic
        val faker = Fakers.faker
    }
}
