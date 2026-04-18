package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redissonClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class RedissonSuspendLeaderElectionSupportTest: AbstractRedissonTest() {

    companion object: KLoggingChannel()

    @Test
    fun `run suspend action if leader`() = runSuspendIO {
        val jobName = randomName()

        val jobs = listOf(
            launch {
                redissonClient.suspendRunIfLeader(jobName) {
                    log.debug { "작업 1 을 시작합니다." }
                    randomDelay(50, 100)
                    log.debug { "작업 1 을 종료합니다." }
                }
            }.log("job1"),

            launch {
                redissonClient.suspendRunIfLeader(jobName) {
                    log.debug { "작업 2 을 시작합니다." }
                    randomDelay(50, 100)
                    log.debug { "작업 2 을 종료합니다." }
                }
            }.log("job2")
        )
        jobs.joinAll()
    }

    private suspend fun randomDelay(from: Long = 5L, until: Long = 10L) {
        delay(Random.nextLong(from, until).milliseconds)
    }
}
