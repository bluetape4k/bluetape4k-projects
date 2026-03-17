package io.bluetape4k.exposed.r2dbc.lettuce

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
import io.lettuce.core.RedisClient

/**
 * exposed-r2dbc-lettuce 통합 테스트 베이스 클래스.
 *
 * - R2DBC 기반 DB (TestDB)
 * - Redis Testcontainers (Lettuce)
 *
 * [redisClient]는 JVM 종료 시 [ShutdownQueue]를 통해 한 번만 shutdown된다.
 * 테스트 클래스에서 직접 `redisClient.shutdown()`을 호출하지 말 것.
 */
abstract class AbstractR2dbcLettuceTest: AbstractExposedR2dbcTest() {
    companion object: KLoggingChannel() {
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

        @JvmStatic
        protected val faker = Fakers.faker

        @JvmStatic
        protected fun randomString(): String = Fakers.randomString(1024, 2048)

        @JvmStatic
        protected fun randomName(): String = "$LibraryName:${Base58.randomString(8)}"

        /**
         * Lettuce 캐시 테스트는 H2 in-memory DB만 사용한다.
         */
        @JvmStatic
        fun enableDialects() = setOf(TestDB.H2)

        const val ENABLE_DIALECTS_METHOD = "enableDialects"
    }
}
