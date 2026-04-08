package io.bluetape4k.exposed.jdbc.caffeine

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging

/**
 * exposed-jdbc-caffeine 통합 테스트 베이스 클래스.
 *
 * - H2_MYSQL, PostgreSQL, MySQL_V8 멀티 DB 지원 (Exposed withTables 패턴)
 * - Redis/Testcontainers 불필요 (Caffeine 로컬 캐시)
 */
abstract class AbstractJdbcCaffeineTest: AbstractExposedTest() {
    companion object: KLogging() {
        @JvmStatic
        protected val faker = Fakers.faker

        /**
         * Caffeine 캐시 테스트는 H2_MYSQL, PostgreSQL, MySQL_V8 DB를 사용합니다.
         */
        @JvmStatic
        fun getEnabledDialects() = setOf(TestDB.H2_MYSQL, TestDB.POSTGRESQL, TestDB.MYSQL_V8)

        const val ENABLE_DIALECTS_METHOD = "getEnabledDialects"
    }
}
