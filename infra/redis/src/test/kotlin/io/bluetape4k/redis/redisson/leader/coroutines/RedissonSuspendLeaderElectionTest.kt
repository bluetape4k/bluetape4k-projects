package io.bluetape4k.redis.redisson.leader.coroutines

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class RedissonSuspendLeaderElectionTest: AbstractRedissonTest() {

    companion object: KLoggingChannel()

    @Test
    fun `run suspend action if leader`() = runSuspendIO {
        val lockName = randomName()
        val leaderElection = RedissonSuspendLeaderElection(redissonClient)

        coroutineScope {
            launch {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 1 을 시작합니다." }
                    delay(10)
                    log.debug { "작업 1 을 종료합니다." }
                }
            }.log("job1")

            launch {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다." }
                    delay(10)
                    log.debug { "작업 2 을 종료합니다." }
                }
            }.log("job2")
        }
    }

    @Test
    fun `run action if leader in multi job`() = runSuspendIO {
        val lockName = randomName()
        val leaderElection = RedissonSuspendLeaderElection(redissonClient)

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerJob = 4

        SuspendedJobTester()
            .numThreads(numThreads)
            .roundsPerJob(numThreads * roundsPerJob)
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 1 을 시작합니다." }
                    delay(Random.nextLong(5, 10))
                    task1.incrementAndGet()
                    log.debug { "작업 1 을 종료합니다." }
                }
            }
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다." }
                    delay(Random.nextLong(5, 10))
                    task2.incrementAndGet()
                    log.debug { "작업 2 을 종료합니다." }
                }
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerJob
        task2.get() shouldBeEqualTo numThreads * roundsPerJob
    }
}
