package io.bluetape4k.redis.redisson.leader.coroutines

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class RedissonCoLeaderElectionTest: AbstractRedissonTest() {

    companion object: KLogging()

    @Test
    fun `run suspend action if leader`() = runSuspendIO {
        val lockName = randomName()
        val leaderElection = RedissonCoLeaderElection(redissonClient)

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
        val leaderElection = RedissonCoLeaderElection(redissonClient)

        val task1 = atomic(0)
        val task2 = atomic(0)

        MultijobTester()
            .numThreads(4)
            .roundsPerJob(36)
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 1 을 시작합니다." }
                    delay(10)
                    task1.incrementAndGet()
                    log.debug { "작업 1 을 종료합니다." }
                }
            }
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다." }
                    delay(10)
                    task2.incrementAndGet()
                    log.debug { "작업 2 을 종료합니다." }
                }
            }
            .run()

        task1.value shouldBeEqualTo 4 * 36 / 2    // Job 1 과 Job 2 가 공평하게 실행되어야 한다.
        task2.value shouldBeEqualTo 4 * 36 / 2    // Job 1 과 Job 2 가 공평하게 실행되어야 한다.
    }
}
