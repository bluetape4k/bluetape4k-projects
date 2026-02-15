package io.bluetape4k.avro

import io.bluetape4k.avro.AbstractAvroTest.Companion.faker
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

/**
 * Avro 관련 테스트의 기본 추상 클래스입니다.
 *
 * 테스트에서 공통적으로 사용하는 [faker] 인스턴스와 로깅 기능을 제공합니다.
 * 모든 Avro 테스트 클래스는 이 클래스를 상속하여 일관된 테스트 환경을 구성합니다.
 */
abstract class AbstractAvroTest {

    companion object: KLogging() {
        /**
         * 테스트 데이터 생성을 위한 Faker 인스턴스
         */
        @JvmStatic
        val faker = Fakers.faker
    }
}
