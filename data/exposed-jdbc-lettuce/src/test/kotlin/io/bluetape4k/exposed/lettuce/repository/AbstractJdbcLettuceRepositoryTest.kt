package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
import io.lettuce.core.RedisClient

/**
 * exposed-jdbc-lettuce 통합 테스트 베이스 클래스.
 *
 * - H2 in-memory DB (Exposed DSL)
 * - Redis Testcontainers (Lettuce)
 *
 * [redisClient]는 JVM 종료 시 [ShutdownQueue]를 통해 한 번만 shutdown된다.
 * 테스트 클래스에서 직접 `redisClient.shutdown()`을 호출하지 말 것.
 */
abstract class AbstractJdbcLettuceRepositoryTest {
    companion object : KLogging() {
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        val redisClient: RedisClient by lazy {
            LettuceClients
                .clientOf(redis.host, redis.port)
                .apply {
                    ShutdownQueue.register { shutdown() }
                }
        }
    }
}
