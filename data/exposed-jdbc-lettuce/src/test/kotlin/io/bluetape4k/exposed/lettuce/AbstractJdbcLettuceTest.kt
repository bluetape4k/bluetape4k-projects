package io.bluetape4k.exposed.lettuce

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
import io.lettuce.core.RedisClient

/**
 * exposed-jdbc-lettuce 통합 테스트 베이스 클래스.
 *
 * - H2, PostgreSQL, MySQL 멀티 DB 지원 (Exposed withTables 패턴)
 * - Redis Testcontainers (Lettuce)
 *
 * [redisClient]는 JVM 종료 시 [io.bluetape4k.utils.ShutdownQueue]를 통해 한 번만 shutdown된다.
 * 테스트 클래스에서 직접 `redisClient.shutdown()`을 호출하지 말 것.
 */
abstract class AbstractJdbcLettuceTest : AbstractExposedTest() {
    companion object : KLogging() {

        @JvmStatic
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        @JvmStatic
        val redisClient: RedisClient by lazy {
            LettuceClients
                .clientOf(redis.host, redis.port)
                .apply {
                    ShutdownQueue.register { shutdown() }
                }
        }

        // AbstractExposedTest.Companion.faker 는 이미 상속으로 제공됩니다.

        @JvmStatic
        protected fun randomString(): String =
            Fakers.randomString(1024, 2048)

        @JvmStatic
        protected fun randomName(): String = "${LibraryName}:${Base58.randomString(8)}"

        const val ENABLE_DIALECTS_METHOD = "getEnabledDialects"

        @JvmStatic
        fun getEnabledDialects() = setOf(TestDB.H2_MYSQL, TestDB.POSTGRESQL, TestDB.MYSQL_V8)
    }
}
