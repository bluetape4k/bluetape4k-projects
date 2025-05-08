package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.concurrent.futureOf
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class RedissonLeaderElectionTest: AbstractRedissonTest() {

    companion object: KLogging()

    @Test
    fun `run action if leader`() {
        val lockName = randomName()
        val leaderElection = RedissonLeaderElection(redissonClient)

        val executor = Executors.newFixedThreadPool(Runtimex.availableProcessors)
        try {
            val countDownLatch = CountDownLatch(2)

            executor.run {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 1 을 시작합니다." }
                    Thread.sleep(100)
                    log.debug { "작업 1 을 종료합니다." }
                    countDownLatch.countDown()
                }
            }
            executor.run {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다." }
                    Thread.sleep(100)
                    log.debug { "작업 2 을 종료합니다." }
                    countDownLatch.countDown()
                }
            }

            countDownLatch.await()
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `run async action if leader`() {
        val lockName = randomName()
        val leaderElection = RedissonLeaderElection(redissonClient)
        val countDownLatch = CountDownLatch(2)

        val future1 = futureOf {
            leaderElection.runAsyncIfLeader(lockName) {
                futureOf {
                    log.debug { "작업 1 을 시작합니다." }
                    Thread.sleep(100)
                    log.debug { "작업 1 을 종료합니다." }
                    Thread.sleep(10)
                    countDownLatch.countDown()
                    42
                }
            }.join()
        }
        val future2 = futureOf {
            leaderElection.runAsyncIfLeader(lockName) {
                futureOf {
                    log.debug { "작업 2 을 시작합니다." }
                    Thread.sleep(100)
                    log.debug { "작업 2 을 종료합니다." }
                    Thread.sleep(10)
                    countDownLatch.countDown()
                    43
                }
            }.join()
        }
        countDownLatch.await(5, TimeUnit.SECONDS)
        future1.get() shouldBeEqualTo 42
        future2.get() shouldBeEqualTo 43
    }

    @Test
    fun `run action if leader in multi threading`() {
        val lockName = randomName()
        val leaderElection = RedissonLeaderElection(redissonClient)

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        MultithreadingTester()
            .numThreads(numThreads)
            .roundsPerThread(roundsPerThread)
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                    task1.incrementAndGet()
                    log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                    Thread.sleep(Random.nextLong(5, 10))
                }
            }
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                    task2.incrementAndGet()
                    log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                    Thread.sleep(Random.nextLong(5, 10))
                }
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }

    @Test
    fun `run action if leader in virtual threads`() {
        val lockName = randomName()
        val leaderElection = RedissonLeaderElection(redissonClient)

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        StructuredTaskScopeTester()
            .roundsPerTask(numThreads * roundsPerThread / 2)
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                    task1.incrementAndGet()
                    log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                    Thread.sleep(Random.nextLong(5, 10))
                }
            }
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                    task2.incrementAndGet()
                    log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                    Thread.sleep(Random.nextLong(5, 10))
                }
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }

    @Test
    fun `run async action if leader in multi threading`() {
        val lockName = randomName()
        val leaderElection = RedissonLeaderElection(redissonClient)

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        MultithreadingTester()
            .numThreads(numThreads)
            .roundsPerThread(roundsPerThread)
            .add {
                leaderElection.runAsyncIfLeader(lockName) {
                    futureOf {
                        log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                        task1.incrementAndGet()
                        log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                        Thread.sleep(Random.nextLong(5, 10))
                        42
                    }
                }.join()
            }
            .add {
                leaderElection.runAsyncIfLeader(lockName) {
                    futureOf {
                        log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                        task2.incrementAndGet()
                        log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                        Thread.sleep(Random.nextLong(5, 10))
                        43
                    }
                }.join()
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }

    @Test
    fun `run async action if leader in virtual threads`() {
        val lockName = randomName()
        val leaderElection = RedissonLeaderElection(redissonClient)

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        StructuredTaskScopeTester()
            .roundsPerTask(numThreads * roundsPerThread / 2)
            .add {
                leaderElection.runAsyncIfLeader(lockName, VirtualThreadExecutor) {
                    futureOf {
                        log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                        task1.incrementAndGet()
                        log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                        Thread.sleep(Random.nextLong(5, 10))
                        42
                    }
                }.join()
            }
            .add {
                leaderElection.runAsyncIfLeader(lockName, VirtualThreadExecutor) {
                    futureOf {
                        log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                        task2.incrementAndGet()
                        log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                        Thread.sleep(Random.nextLong(5, 10))
                        43
                    }
                }.join()
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }
}
