package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.concurrent.futureOf
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.redisson.client.RedisException
import java.time.Duration
import java.util.concurrent.CompletionException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.random.Random

class RedissonLeaderGroupElectionTest: AbstractRedissonTest() {

    companion object: KLogging()

    private val options = LeaderGroupElectionOptions(
        maxLeaders = 3,
        waitTime = Duration.ofSeconds(30),
        leaseTime = Duration.ofSeconds(60),

        )
    private val election by lazy { RedissonLeaderGroupElection(redissonClient, options) }

    // ── 기본 동작 ──────────────────────────────────────────────────────────

    @Test
    fun `runIfLeader - 리더로 선출되어 action 을 실행하고 결과를 반환한다`() {
        val result = election.runIfLeader(randomName()) { "hello" }
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `runIfLeader - 서로 다른 lockName 은 독립적인 슬롯 풀을 가진다`() {
        val result1 = election.runIfLeader(randomName()) { "a" }
        val result2 = election.runIfLeader(randomName()) { "b" }

        result1 shouldBeEqualTo "a"
        result2 shouldBeEqualTo "b"
    }

    @Test
    fun `runIfLeader - action 예외 발생 시 예외가 호출자에게 전파된다`() {
        assertThrows<RuntimeException> {
            election.runIfLeader(randomName()) { throw RuntimeException("테스트 예외") }
        }
    }

    @Test
    fun `runIfLeader - action 예외 발생 후에도 슬롯이 반환되어 다음 호출이 성공한다`() {
        val lockName = randomName()
        runCatching { election.runIfLeader(lockName) { throw RuntimeException("실패") } }

        val result = election.runIfLeader(lockName) { "복구 성공" }
        result shouldBeEqualTo "복구 성공"
    }

    @Test
    fun `runIfLeader - maxLeaders 슬롯이 모두 사용 중이면 waitTime 초과 시 RedisException 이 발생한다`() {
        val shortWaitOptions = LeaderGroupElectionOptions(maxLeaders = 1, waitTime = Duration.ofMillis(100))
        val singleElection = RedissonLeaderGroupElection(redissonClient, shortWaitOptions)
        val lockName = randomName()
        val acquiredLatch = CountDownLatch(1)
        val holdLatch = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()

        executor.submit {
            singleElection.runIfLeader(lockName) {
                acquiredLatch.countDown()
                holdLatch.await()
            }
        }

        try {
            acquiredLatch.await(2, TimeUnit.SECONDS)
            assertThrows<RedisException> {
                singleElection.runIfLeader(lockName) { }
            }
        } finally {
            holdLatch.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `maxLeaders=1 이면 LeaderElection 과 동일하게 직렬 실행된다`() {
        val oneLeader = options.copy(maxLeaders = 1)
        val singleElection = RedissonLeaderGroupElection(redissonClient, oneLeader)
        val lockName = randomName()
        val counter = AtomicInteger(0)
        val numThreads = 6

        MultithreadingTester()
            .workers(numThreads)
            .rounds(2)
            .add { singleElection.runIfLeader(lockName) { counter.incrementAndGet() } }
            .run()

        counter.get() shouldBeEqualTo numThreads * 2
    }

    // ── runIfLeader Virtual Thread ────────────────────────────────────────

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `runIfLeader - Virtual Thread 에서 동시 실행 중인 리더 수가 maxLeaders 를 초과하지 않는다`() {
        val lockName = randomName()
        val currentConcurrent = AtomicInteger(0)
        val peakConcurrent = AtomicInteger(0)

        StructuredTaskScopeTester()
            .rounds(options.maxLeaders * 8)
            .add {
                election.runIfLeader(lockName) {
                    val current = currentConcurrent.incrementAndGet()
                    peakConcurrent.updateAndGet { max(it, current) }
                    Thread.sleep(Random.nextLong(5, 15))
                    currentConcurrent.decrementAndGet()
                }
            }
            .run()

        log.debug { "최대 동시 실행 수: ${peakConcurrent.get()} / maxLeaders=${options.maxLeaders}" }
        peakConcurrent.get() shouldBeLessOrEqualTo options.maxLeaders
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `멀티스레드 스트레스 - runIfLeader Virtual Thread 에서 모든 실행이 완료되고 카운터가 정확하다`() {
        val lockName = randomName()
        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        StructuredTaskScopeTester()
            .rounds(numThreads * roundsPerThread / 2)
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "작업 1. task1=${task1.get()}" }
                    Thread.sleep(Random.nextLong(1, 5))
                    task1.incrementAndGet()
                }
            }
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "작업 2. task2=${task2.get()}" }
                    Thread.sleep(Random.nextLong(1, 5))
                    task2.incrementAndGet()
                }
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }

    // ── 동시 실행 제한 ────────────────────────────────────────────────────

    @Test
    fun `동시 실행 중인 리더 수가 maxLeaders 를 초과하지 않는다`() {
        val lockName = randomName()
        val currentConcurrent = AtomicInteger(0)
        val peakConcurrent = AtomicInteger(0)

        MultithreadingTester()
            .workers(options.maxLeaders * 4)
            .rounds(2)
            .add {
                election.runIfLeader(lockName) {
                    val current = currentConcurrent.incrementAndGet()
                    peakConcurrent.updateAndGet { max(it, current) }
                    Thread.sleep(Random.nextLong(5, 15))
                    currentConcurrent.decrementAndGet()
                }
            }
            .run()

        log.debug { "최대 동시 실행 수: ${peakConcurrent.get()} / maxLeaders=${options.maxLeaders}" }
        peakConcurrent.get() shouldBeLessOrEqualTo options.maxLeaders
    }

    // ── 상태 정보 ────────────────────────────────────────────────────────

    @Test
    fun `state - 초기 상태는 activeCount=0, isFull=false, isEmpty=true 이다`() {
        val lockName = randomName()
        val state = election.state(lockName)

        state.lockName shouldBeEqualTo lockName
        state.maxLeaders shouldBeEqualTo options.maxLeaders
        state.activeCount shouldBeEqualTo 0
        state.availableSlots shouldBeEqualTo options.maxLeaders
        state.isEmpty.shouldBeTrue()
        state.isFull.shouldBeFalse()
    }

    @Test
    fun `state - maxLeaders 슬롯이 모두 사용 중이면 isFull=true 이고 해제 후 isEmpty=true 이다`() {
        val lockName = randomName()
        val acquiredLatch = CountDownLatch(options.maxLeaders)
        val holdLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(options.maxLeaders)

        repeat(options.maxLeaders) {
            executor.submit {
                election.runIfLeader(lockName) {
                    acquiredLatch.countDown()
                    holdLatch.await()
                }
            }
        }

        acquiredLatch.await(5, TimeUnit.SECONDS)
        election.state(lockName).isFull.shouldBeTrue()
        election.activeCount(lockName) shouldBeEqualTo options.maxLeaders
        election.availableSlots(lockName) shouldBeEqualTo 0

        holdLatch.countDown()
        executor.shutdown()
        executor.awaitTermination(3, TimeUnit.SECONDS)

        election.state(lockName).isEmpty.shouldBeTrue()
    }

    // ── 스트레스 테스트 ────────────────────────────────────────────────────

    @Test
    fun `멀티스레드 스트레스 - 모든 실행이 완료되고 카운터가 정확하다`() {
        val lockName = randomName()
        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        MultithreadingTester()
            .workers(numThreads)
            .rounds(roundsPerThread)
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "작업 1. task1=${task1.get()}" }
                    Thread.sleep(Random.nextLong(1, 5))
                    task1.incrementAndGet()
                }
            }
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "작업 2. task2=${task2.get()}" }
                    Thread.sleep(Random.nextLong(1, 5))
                    task2.incrementAndGet()
                }
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }

    // ── runAsyncIfLeader 기본 동작 ────────────────────────────────────────

    @Test
    fun `runAsyncIfLeader - 리더로 선출되어 action 을 실행하고 결과를 반환한다`() {
        val result = election.runAsyncIfLeader(randomName()) {
            futureOf { "hello" }
        }.join()
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `runAsyncIfLeader - action 예외 발생 후에도 슬롯이 반환되어 다음 호출이 성공한다`() {
        val lockName = randomName()
        runCatching {
            election.runAsyncIfLeader(lockName) {
                futureOf<Int> { throw RuntimeException("실패") }
            }.join()
        }

        val result = election.runAsyncIfLeader(lockName) { futureOf { "복구 성공" } }.join()
        result shouldBeEqualTo "복구 성공"
    }

    @Test
    fun `runAsyncIfLeader - failed future 발생 시 CompletionException 으로 전파되고 슬롯이 반환된다`() {
        val lockName = randomName()

        assertThrows<CompletionException> {
            election.runAsyncIfLeader(lockName) {
                futureOf<Int> { throw IllegalStateException("boom") }
            }.join()
        }

        // 슬롯이 반환되어 다음 호출이 성공해야 함
        val result = election.runAsyncIfLeader(lockName) { futureOf { 42 } }.join()
        result shouldBeEqualTo 42
    }

    // ── runAsyncIfLeader 동시 실행 제한 ──────────────────────────────────

    @Test
    fun `runAsyncIfLeader - 동시 실행 중인 리더 수가 maxLeaders 를 초과하지 않는다`() {
        val lockName = randomName()
        val currentConcurrent = AtomicInteger(0)
        val peakConcurrent = AtomicInteger(0)

        MultithreadingTester()
            .workers(options.maxLeaders * 4)
            .rounds(2)
            .add {
                election.runAsyncIfLeader(lockName) {
                    futureOf {
                        val current = currentConcurrent.incrementAndGet()
                        peakConcurrent.updateAndGet { max(it, current) }
                        Thread.sleep(Random.nextLong(5, 15))
                        currentConcurrent.decrementAndGet()
                    }
                }.join()
            }
            .run()

        log.debug { "최대 동시 실행 수: ${peakConcurrent.get()} / maxLeaders=${options.maxLeaders}" }
        peakConcurrent.get() shouldBeLessOrEqualTo options.maxLeaders
    }

    @Test
    fun `멀티스레드 스트레스 - runAsyncIfLeader 모든 실행이 완료되고 카운터가 정확하다`() {
        val lockName = randomName()
        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        MultithreadingTester()
            .workers(numThreads)
            .rounds(roundsPerThread)
            .add {
                election.runAsyncIfLeader(lockName) {
                    futureOf {
                        log.debug { "비동기 작업 1. task1=${task1.get()}" }
                        Thread.sleep(Random.nextLong(1, 5))
                        task1.incrementAndGet()
                    }
                }.join()
            }
            .add {
                election.runAsyncIfLeader(lockName) {
                    futureOf {
                        log.debug { "비동기 작업 2. task2=${task2.get()}" }
                        Thread.sleep(Random.nextLong(1, 5))
                        task2.incrementAndGet()
                    }
                }.join()
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }

    // ── runAsyncIfLeader Virtual Thread ──────────────────────────────────

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `runAsyncIfLeader - Virtual Thread 에서 동시 실행 중인 리더 수가 maxLeaders 를 초과하지 않는다`() {
        val lockName = randomName()
        val currentConcurrent = AtomicInteger(0)
        val peakConcurrent = AtomicInteger(0)

        StructuredTaskScopeTester()
            .rounds(options.maxLeaders * 8)
            .add {
                election.runAsyncIfLeader(lockName, VirtualThreadExecutor) {
                    futureOf {
                        val current = currentConcurrent.incrementAndGet()
                        peakConcurrent.updateAndGet { max(it, current) }
                        Thread.sleep(Random.nextLong(5, 15))
                        currentConcurrent.decrementAndGet()
                    }
                }.join()
            }
            .run()

        log.debug { "최대 동시 실행 수: ${peakConcurrent.get()} / maxLeaders=${options.maxLeaders}" }
        peakConcurrent.get() shouldBeLessOrEqualTo options.maxLeaders
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `멀티스레드 스트레스 - runAsyncIfLeader Virtual Thread 에서 모든 실행이 완료되고 카운터가 정확하다`() {
        val lockName = randomName()
        val task1 = AtomicInteger(0)
        val task2 = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        StructuredTaskScopeTester()
            .rounds(numThreads * roundsPerThread / 2)
            .add {
                election.runAsyncIfLeader(lockName, VirtualThreadExecutor) {
                    futureOf {
                        log.debug { "비동기 작업 1. task1=${task1.get()}" }
                        Thread.sleep(Random.nextLong(1, 5))
                        task1.incrementAndGet()
                    }
                }.join()
            }
            .add {
                election.runAsyncIfLeader(lockName, VirtualThreadExecutor) {
                    futureOf {
                        log.debug { "비동기 작업 2. task2=${task2.get()}" }
                        Thread.sleep(Random.nextLong(1, 5))
                        task2.incrementAndGet()
                    }
                }.join()
            }
            .run()

        task1.get() shouldBeEqualTo numThreads * roundsPerThread / 2
        task2.get() shouldBeEqualTo numThreads * roundsPerThread / 2
    }
}
