package io.bluetape4k.quarkus.tests.resources

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.quarkus.tests.AbstractQuarkusTest
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import org.redisson.api.redisnode.RedisNodes

/**
 * Redis 사용하는 데 `redisson-quarkus-30` 라이브러리를 활용하여, quarkus framework에서 redisson 을 활용하는 예제입니다.
 * bluetape4k-testcontainers의 [RedisServer]를 testcontainers로 띄우고,
 * 환경설정에서 `testcontainers.redis.url` 을 이용하여 redisson 이 접속할 주소를 지정합니다.
 *
 * 참고: application.properties
 *
 * ```
 * %test.quarkus.redisson.single-server-config.address=${testcontainers.redis.url}
 * %test.quarkus.redisson.single-server-config.password=null
 * %test.quarkus.redisson.threads=16
 * %test.quarkus.redisson.netty-threads=32
 * ```
 */
@QuarkusTest
@QuarkusTestResource(RedisTestResource::class)
class RedisTestResourceTest: AbstractQuarkusTest() {

    companion object: KLogging()

    @Inject
    internal lateinit var redisson: RedissonClient

    @Test
    fun `context loading`() {
        ::redisson.isInitialized.shouldNotBeNull()
    }

    @Test
    fun `ping redis`() {
        val redisSingle = redisson.getRedisNodes(RedisNodes.SINGLE)
        redisSingle.pingAll().shouldBeTrue()
    }

    @Test
    fun `use redisson map synchronously`() {
        val mapName = Fakers.fixedString(12)
        val rmap = redisson.getMap<String, String>(mapName)
        rmap["key"] = "value"
        rmap["key"] shouldBeEqualTo "value"
        rmap.clear()
    }

    @Test
    fun `use redisson map asynchronously`() = runTest {
        val mapName = Fakers.fixedString(12)
        val rmap = redisson.getMap<String, String>(mapName)

        log.debug { "Put asynchronously" }
        rmap.fastPutAsync("key", "value").suspendAwait().shouldBeTrue()

        log.debug { "Get asynchronously" }
        rmap.getAsync("key").suspendAwait() shouldBeEqualTo "value"
    }
}
