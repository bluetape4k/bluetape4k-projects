package io.bluetape4k.exposed.jdbc.caffeine

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

/**
 * exposed-jdbc-caffeine 통합 테스트 베이스 클래스.
 *
 * - JDBC 기반 DB (TestDB) -- H2 in-memory만 사용
 * - Redis/Testcontainers 불필요 (Caffeine 로컬 캐시)
 */
abstract class AbstractJdbcCaffeineTest: AbstractExposedTest() {
    companion object: KLogging() {
        @JvmStatic
        protected val faker = Fakers.faker

        /**
         * Caffeine 캐시 테스트는 H2 in-memory DB만 사용합니다.
         */
        @JvmStatic
        fun getEnabledDialects() = setOf(TestDB.H2)

        const val ENABLE_DIALECTS_METHOD = "getEnabledDialects"
    }
}
