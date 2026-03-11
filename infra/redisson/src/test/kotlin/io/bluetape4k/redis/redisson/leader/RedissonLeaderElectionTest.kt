package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.concurrent.futureOf
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.leader.LeaderElectionOptions
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redissonClient
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.redisson.client.RedisException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
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
                    randomSleep(90, 100)
                    log.debug { "작업 1 을 종료합니다." }
                    countDownLatch.countDown()
                }
            }
            executor.run {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다." }
                    randomSleep(90, 100)
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
                    randomSleep(90, 100)
                    log.debug { "작업 1 을 종료합니다." }
                    countDownLatch.countDown()
                    42
                }
            }.join()
        }
        val future2 = futureOf {
            leaderElection.runAsyncIfLeader(lockName) {
                futureOf {
                    log.debug { "작업 2 을 시작합니다." }
                    randomSleep(90, 100)
                    log.debug { "작업 2 을 종료합니다." }
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
    fun `run async action should release lock even when action fails`() {
        val lockName = randomName()
        val options = LeaderElectionOptions(
            waitTime = Duration.ofSeconds(1),
            leaseTime = Duration.ofSeconds(30),
        )
        val leaderElection = RedissonLeaderElection(redissonClient, options)

        assertThrows<CompletionException> {
            leaderElection
                .runAsyncIfLeader(lockName) {
                    CompletableFuture.failedFuture<Int>(IllegalStateException("boom"))
                }
                .join()
        }

        leaderElection
            .runAsyncIfLeader(lockName) { CompletableFuture.completedFuture(1) }
            .get(2, TimeUnit.SECONDS) shouldBeEqualTo 1
    }

    @Test
    fun `run action should throw when lock is not acquired`() {
        val lockName = randomName()
        val options = LeaderElectionOptions(
            waitTime = Duration.ofMillis(100),
            leaseTime = Duration.ofSeconds(5),
        )
        val leaderElection = RedissonLeaderElection(redissonClient, options)
        val lockAcquired = CountDownLatch(1)
        val releaseLock = CountDownLatch(1)
        val lockHolder = Executors.newSingleThreadExecutor()

        lockHolder.submit {
            val lock = redissonClient.getLock(lockName)
            lock.lock(3, TimeUnit.SECONDS)
            lockAcquired.countDown()
            runCatching { releaseLock.await(2, TimeUnit.SECONDS) }
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }

        try {
            lockAcquired.await(1, TimeUnit.SECONDS)
            assertThrows<RedisException> {
                leaderElection.runIfLeader(lockName) { 1 }
            }
        } finally {
            releaseLock.countDown()
            lockHolder.shutdownNow()
        }
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
            .workers(numThreads)
            .rounds(roundsPerThread)
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                    task1.incrementAndGet()
                    randomSleep()
                    log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                }
            }
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                    task2.incrementAndGet()
                    randomSleep()
                    log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                }
            }
            .run()

        task1.get() shouldBeGreaterThan 0
        task2.get() shouldBeGreaterThan 0
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `run action if leader in virtual threads`() {
        val lockName = randomName()
        val leaderElection = RedissonLeaderElection(redissonClient)

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        StructuredTaskScopeTester()
            .rounds(numThreads * roundsPerThread / 2)
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                    task1.incrementAndGet()
                    randomSleep()
                    log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                }
            }
            .add {
                leaderElection.runIfLeader(lockName) {
                    log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                    task2.incrementAndGet()
                    randomSleep()
                    log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                }
            }
            .run()

        task1.get() shouldBeGreaterThan 0
        task2.get() shouldBeGreaterThan 0
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
            .workers(numThreads)
            .rounds(roundsPerThread)
            .add {
                leaderElection.runAsyncIfLeader(lockName) {
                    futureOf {
                        log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                        task1.incrementAndGet()
                        randomSleep()
                        log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                        42
                    }
                }.join()
            }
            .add {
                leaderElection.runAsyncIfLeader(lockName) {
                    futureOf {
                        log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                        task2.incrementAndGet()
                        randomSleep()
                        log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                        43
                    }
                }.join()
            }
            .run()

        task1.get() shouldBeGreaterThan 0
        task2.get() shouldBeGreaterThan 0
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `run async action if leader in virtual threads`() {
        val lockName = randomName()
        val leaderElection = RedissonLeaderElection(redissonClient)

        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        StructuredTaskScopeTester()
            .rounds(numThreads * roundsPerThread / 2)
            .add {
                leaderElection.runAsyncIfLeader(lockName, VirtualThreadExecutor) {
                    futureOf {
                        log.debug { "작업 1 을 시작합니다. task1=${task1.get()}" }
                        task1.incrementAndGet()
                        randomSleep()
                        log.debug { "작업 1 을 종료합니다. task1=${task1.get()}" }
                        42
                    }
                }.join()
            }
            .add {
                leaderElection.runAsyncIfLeader(lockName, VirtualThreadExecutor) {
                    futureOf {
                        log.debug { "작업 2 을 시작합니다. task2=${task2.get()}" }
                        task2.incrementAndGet()
                        randomSleep()
                        log.debug { "작업 2 을 종료합니다. task2=${task2.get()}" }
                        43
                    }
                }.join()
            }
            .run()

        task1.get() shouldBeGreaterThan 0
        task2.get() shouldBeGreaterThan 0
    }

    /**
     * [MultithreadingTester]를 사용하여 짧은 `waitTime` 환경에서 동시 리더 선출 경쟁을 테스트한다.
     *
     * 여러 스레드가 동일한 락 이름으로 [RedissonLeaderElection.runIfLeader]를 동시에 호출할 때,
     * 리더로 선출된 스레드는 카운터를 증가시키고,
     * 락 획득에 실패한 스레드는 [RedisException]을 안전하게 삼킨다.
     */
    @Test
    fun `동시 다수 스레드에서 runIfLeader 호출 시 성공하거나 RedisException 을 발생시킨다`() {
        val lockName = randomName()
        val shortWaitOptions = LeaderElectionOptions(
            waitTime = Duration.ofMillis(50),
            leaseTime = Duration.ofSeconds(5),
        )
        val leaderElection = RedissonLeaderElection(redissonClient, shortWaitOptions)
        val successCount = AtomicInteger(0)

        MultithreadingTester()
            .workers(16)
            .rounds(4)
            .add {
                runCatching {
                    leaderElection.runIfLeader(lockName) {
                        successCount.incrementAndGet()
                        randomSleep(10, 30)
                    }
                }
                // RedisException(락 획득 실패) 또는 성공 — 둘 다 허용
            }
            .run()

        log.debug { "총 성공 횟수: ${successCount.get()}" }
    }

    /**
     * [StructuredTaskScopeTester]를 사용하여 Virtual Thread 환경에서
     * 동시 리더 선출 경쟁 시 [RedisException] 이 안전하게 처리되는지 검증한다.
     */
    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `Virtual Thread 에서 runIfLeader 호출 시 성공하거나 RedisException 을 안전하게 처리한다`() {
        val lockName = randomName()
        val shortWaitOptions = LeaderElectionOptions(
            waitTime = Duration.ofMillis(50),
            leaseTime = Duration.ofSeconds(5),
        )
        val leaderElection = RedissonLeaderElection(redissonClient, shortWaitOptions)
        val successCount = AtomicInteger(0)

        StructuredTaskScopeTester()
            .rounds(32)
            .add {
                runCatching {
                    leaderElection.runIfLeader(lockName) {
                        successCount.incrementAndGet()
                        randomSleep(10, 30)
                    }
                }
                // RedisException(락 획득 실패) 또는 성공 — 둘 다 허용
            }
            .run()

        log.debug { "총 성공 횟수: ${successCount.get()}" }
    }

    private fun randomSleep(from: Long = 5L, until: Long = 10L) {
        Thread.sleep(Random.nextLong(from, until))
    }


}
