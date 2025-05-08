package io.bluetape4k.redis.redisson.coroutines

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.leader.coroutines.RedissonCoLeaderElection
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class RedissonClientCoroutineTest: AbstractRedissonCoroutineTest() {

    companion object: KLogging()

    @Test
    fun `with batch async`() = runSuspendIO {
        // create map
        val mapName = randomName()
        val map = redisson.getMap<String, String>(mapName)

        try {

            val result = redisson.withSuspendedBatch {
                val batchMap = getMap<String, String>(mapName)
                batchMap.fastPutAsync("1", "2")
                batchMap.putAsync("2", "5")
                batchMap.getAllAsync(setOf("1", "2"))
            }
            log.debug { "responses=${result.responses}" }
            result.responses.first() shouldBeEqualTo true
            result.responses.last() shouldBeEqualTo mapOf("1" to "2", "2" to "5")

        } finally {
            map.delete()
        }
    }

    @Test
    fun `use transaction async`() = runSuspendIO {
        val map = redisson.getMap<String, String>(randomName())
        val set = redisson.getSet<String>(randomName())

        try {
            val value: String = randomString(32)
            redisson.withSuspendedTransaction {
                map.putAsync("1", value).coAwait()
                map.getAsync("3").coAwait()

                set.addAsync(value).coAwait()
            }
            map.getAsync("1").coAwait() shouldBeEqualTo value
            set.containsAsync(value).coAwait().shouldBeTrue()

        } finally {
            map.delete()
            set.delete()
        }
    }

    @Test
    fun `use transaction async in multi jobs`() = runSuspendIO {
        val map = redisson.getMap<String, String>(randomName())

        val lockName = randomName()
        val leaderElection = RedissonCoLeaderElection(redissonClient)
        val counter = atomic(0)

        try {
            SuspendedJobTester()
                .numThreads(4)
                .roundsPerJob(32)
                .add {
                    // redisson.runIfLeaderSuspending(lockName) {
                    leaderElection.runIfLeader(lockName) {
                        val value = randomString(64)
                        redisson.withSuspendedTransaction {
                            map.putAsync("1", value).coAwait()
                            map.putAsync("2", value).coAwait()
                            map.putAsync("3", value).coAwait()
                            counter.incrementAndGet()
                        }
                        delay(10L)
                        map.getAsync("1").coAwait() shouldBeEqualTo value
                        map.getAsync("2").coAwait() shouldBeEqualTo value
                        map.getAsync("3").coAwait() shouldBeEqualTo value
                    }
                }
                .run()

            counter.value shouldBeEqualTo 32
        } finally {
            map.delete()
        }
    }
}
