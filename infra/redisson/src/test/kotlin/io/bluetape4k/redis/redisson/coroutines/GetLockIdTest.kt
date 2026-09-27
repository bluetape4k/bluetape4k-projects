package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redissonClient
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue

@DisplayName("RedissonClient.getLockId")
class GetLockIdTest: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `getLockId - 빈 lockName 은 IllegalArgumentException 을 던진다`() {
        assertFailsWith<IllegalArgumentException> { redissonClient.getLockId("") }
        assertFailsWith<IllegalArgumentException> { redissonClient.getLockId("  ") }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `getLockId - 호출마다 단조 증가하는 고유 ID를 반환한다`() {
        val lockName = randomName()

        val id1 = redissonClient.getLockId(lockName)
        val id2 = redissonClient.getLockId(lockName)
        val id3 = redissonClient.getLockId(lockName)

        // Snowflake: 타임스탬프 기반 전역 단조 증가 (정확히 +1 이 아님)
        id2 shouldBeGreaterThan id1
        id3 shouldBeGreaterThan id2
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `getLockId - lockName 에 관계없이 전역 고유 ID를 반환한다`() {
        val nameA = randomName()
        val nameB = randomName()

        val a1 = redissonClient.getLockId(nameA)
        val b1 = redissonClient.getLockId(nameB)
        val a2 = redissonClient.getLockId(nameA)

        // Snowflake: lockName과 무관한 전역 고유 ID — 별도 시퀀스가 없음
        log.debug { "a1=$a1, b1=$b1, a2=$a2" }
        a1 shouldBeGreaterThan 0
        b1 shouldBeGreaterThan 0
        a2 shouldBeGreaterThan a1
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `getLockId - 반환값은 양수이다`() {
        val lockName = randomName()
        val id = redissonClient.getLockId(lockName)
        // Snowflake ID는 항상 양수
        id shouldBeGreaterThan 0
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `getLockId - 동시 호출 시 모든 ID 가 유일하다`() = runSuspendIO {
        val lockName = randomName()
        val concurrency = 64

        val ids = coroutineScope {
            List(concurrency) {
                async {
                    redissonClient.getLockId(lockName)
                }
            }.awaitAll()
        }

        ids shouldHaveSize concurrency
        ids.distinct() shouldHaveSize concurrency // 모두 unique
    }

    @Test
    fun `getLockId - SuspendedJob 에서 동시 호출 시 모든 ID 가 Unique 하다`() = runSuspendIO {
        val lockName = randomName()
        val concurrency = 64
        val ids = ConcurrentLinkedQueue<Long>()

        SuspendedJobTester()
            .workers(2 * Runtimex.availableProcessors)
            .rounds(concurrency)
            .add {
                val id = redissonClient.getLockId(lockName)
                ids.add(id)
            }
            .run()

        ids shouldHaveSize concurrency
        ids.distinct() shouldHaveSize concurrency
    }
}
