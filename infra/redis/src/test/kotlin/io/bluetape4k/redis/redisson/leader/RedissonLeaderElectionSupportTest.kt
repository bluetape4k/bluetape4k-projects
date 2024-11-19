package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.concurrent.futureOf
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.VirtualthreadTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class RedissonLeaderElectionSupportTest: AbstractRedissonTest() {

    companion object: KLogging()

    @Test
    fun `run action if leader`() {
        val lockName = randomName()
        val executor = Executors.newCachedThreadPool()
        val countDownLatch = CountDownLatch(2)

        executor.run {
            redissonClient.runIfLeader(lockName) {
                log.debug { "작업 1 을 시작합니다." }
                Thread.sleep(100)
                log.debug { "작업 1 을 종료합니다." }
                countDownLatch.countDown()
            }
        }
        executor.run {
            redissonClient.runIfLeader(lockName) {
                log.debug { "작업 2 을 시작합니다." }
                Thread.sleep(100)
                log.debug { "작업 2 을 종료합니다." }
                countDownLatch.countDown()
            }
        }

        countDownLatch.await()
        executor.shutdownNow()
    }

    @Test
    fun `run async action if leader`() {
        val lockName = randomName()
        val countDownLatch = CountDownLatch(2)

        val future1 = futureOf {
            redissonClient.runAsyncIfLeader(lockName) {
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
            redissonClient.runAsyncIfLeader(lockName) {
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

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        MultithreadingTester()
            .numThreads(numThreads)
            .roundsPerThread(roundsPerThread)
            .add {
                redissonClient.runIfLeader(lockName) {
                    log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                    task1.incrementAndGet()
                    Thread.sleep(Random.nextLong(5, 10))
                    log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                }
            }
            .add {
                redissonClient.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                    task2.incrementAndGet()
                    Thread.sleep(Random.nextLong(5, 10))
                    log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                }
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }


    @Test
    fun `run action if leader in virtual threading`() {
        val lockName = randomName()

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        VirtualthreadTester()
            .numThreads(numThreads)
            .roundsPerThread(roundsPerThread)
            .add {
                redissonClient.runIfLeader(lockName) {
                    log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                    task1.incrementAndGet()
                    Thread.sleep(Random.nextLong(5, 10))
                    log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                }
            }
            .add {
                redissonClient.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                    task2.incrementAndGet()
                    Thread.sleep(Random.nextLong(5, 10))
                    log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                }
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }

    @Test
    fun `run async action if leader in multi threading`() {
        val lockName = randomName()

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        MultithreadingTester()
            .numThreads(numThreads)
            .roundsPerThread(roundsPerThread)
            .add {
                redissonClient.runAsyncIfLeader(lockName) {
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
                redissonClient.runAsyncIfLeader(lockName) {
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
    fun `run async action if leader in virtual threading`() {
        val lockName = randomName()

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        VirtualthreadTester()
            .numThreads(numThreads)
            .roundsPerThread(roundsPerThread)
            .add {
                redissonClient.runAsyncIfLeader(lockName) {
                    virtualFuture {
                        log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                        task1.incrementAndGet()
                        log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                        Thread.sleep(Random.nextLong(5, 10))
                        42
                    }.toCompletableFuture()
                }.join()
            }
            .add {
                redissonClient.runAsyncIfLeader(lockName) {
                    virtualFuture {
                        log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                        task2.incrementAndGet()
                        log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                        Thread.sleep(Random.nextLong(5, 10))
                        43
                    }.toCompletableFuture()
                }.join()
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }
}
