package io.bluetape4k.exposed.r2dbc.caffeine

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel

/**
 * exposed-r2dbc-caffeine 통합 테스트 베이스 클래스.
 *
 * - R2DBC 기반 DB (TestDB) — H2 in-memory만 사용
 * - Redis/Testcontainers 불필요 (Caffeine 로컬 캐시)
 */
abstract class AbstractR2dbcCaffeineTest: AbstractExposedR2dbcTest() {
    companion object: KLoggingChannel() {
        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String = Fakers.randomString(1024, 2048)

        /**
         * Caffeine 캐시 테스트는 H2 in-memory DB만 사용합니다.
         */
        @JvmStatic
        fun getEnabledDialects() = setOf(TestDB.H2)

        const val ENABLE_DIALECTS_METHOD = "getEnabledDialects"
    }
}
