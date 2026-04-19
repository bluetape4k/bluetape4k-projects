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
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldThrow
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
    fun `getLockId - 같은 lockName 에서 호출마다 단조 증가한다`() {
        val lockName = randomName()

        val id1 = redissonClient.getLockId(lockName)
        val id2 = redissonClient.getLockId(lockName)
        val id3 = redissonClient.getLockId(lockName)

        (id2 > id1).shouldBeTrue()
        (id3 > id2).shouldBeTrue()
        id2 shouldBeEqualTo id1 + 1
        id3 shouldBeEqualTo id2 + 1
    }

    @Test
    fun `getLockId - 다른 lockName 은 독립적인 시퀀스를 가진다`() {
        val nameA = randomName()
        val nameB = randomName()

        val a1 = redissonClient.getLockId(nameA)
        val b1 = redissonClient.getLockId(nameB)
        val a2 = redissonClient.getLockId(nameA)

        a2 shouldBeEqualTo a1 + 1
        // nameB 시퀀스는 nameA 와 독립적으로 증가
        (b1 >= 0).shouldBeTrue()
    }

    @Test
    fun `getLockId - 초기값은 System currentTimeMillis 이상이다`() {
        val lockName = randomName()
        val beforeMillis = System.currentTimeMillis()
        val id = redissonClient.getLockId(lockName)
        // 최초 CAS 로 currentTimeMillis 가 seed 되고 andIncrement 로 seed 값을 반환한다
        id.shouldBeGreaterOrEqualTo(beforeMillis)
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
