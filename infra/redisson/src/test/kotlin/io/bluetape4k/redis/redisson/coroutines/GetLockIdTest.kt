package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redissonClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import io.bluetape4k.assertions.invoking
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RedissonClient.getLockId")
class GetLockIdTest {

    companion object: KLogging()

    @Test
    fun `getLockId - 빈 lockName 은 IllegalArgumentException 을 던진다`() {
        invoking { redissonClient.getLockId("") } shouldThrow IllegalArgumentException::class
        invoking { redissonClient.getLockId("  ") } shouldThrow IllegalArgumentException::class
    }

    @Test
    fun `getLockId - 호출마다 단조 증가하는 고유 ID를 반환한다`() {
        val lockName = randomName()

        val id1 = redissonClient.getLockId(lockName)
        val id2 = redissonClient.getLockId(lockName)
        val id3 = redissonClient.getLockId(lockName)

        // Snowflake: 타임스탬프 기반 전역 단조 증가 (정확히 +1 이 아님)
        (id2 > id1).shouldBeTrue()
        (id3 > id2).shouldBeTrue()
    }

    @Test
    fun `getLockId - lockName 에 관계없이 전역 고유 ID를 반환한다`() {
        val nameA = randomName()
        val nameB = randomName()

        val a1 = redissonClient.getLockId(nameA)
        val b1 = redissonClient.getLockId(nameB)
        val a2 = redissonClient.getLockId(nameA)

        // Snowflake: lockName과 무관한 전역 고유 ID — 별도 시퀀스가 없음
        (a1 > 0).shouldBeTrue()
        (b1 > 0).shouldBeTrue()
        (a2 > a1).shouldBeTrue()
    }

    @Test
    fun `getLockId - 반환값은 양수이다`() {
        val lockName = randomName()
        val id = redissonClient.getLockId(lockName)
        // Snowflake ID는 항상 양수
        (id > 0).shouldBeTrue()
    }

    @Test
    fun `getLockId - 동시 호출 시 모든 ID 가 유일하다`() = runSuspendIO {
        val lockName = randomName()
        val concurrency = 64

        val ids = coroutineScope {
            (1..concurrency).map {
                async(Dispatchers.IO) {
                    withContext(Dispatchers.IO) {
                        redissonClient.getLockId(lockName)
                    }
                }
            }.awaitAll()
        }

        ids.shouldHaveSize(concurrency)
        ids.toSet().shouldHaveSize(concurrency) // 모두 unique
    }
}
