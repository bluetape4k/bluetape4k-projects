package io.bluetape4k.leader.local

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class LocalLeaderElectionTest {

    companion object: KLogging()

    private val election = LocalLeaderElection()

    private fun randomLockName() = "lock-${UUID.randomUUID()}"

    @Test
    fun `runIfLeader - 리더로 선출되어 action 을 실행하고 결과를 반환한다`() {
        val result = election.runIfLeader(randomLockName()) { "hello" }
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `runIfLeader - 서로 다른 lockName 은 독립적으로 실행된다`() {
        val result1 = election.runIfLeader(randomLockName()) { "a" }
        val result2 = election.runIfLeader(randomLockName()) { "b" }

        result1 shouldBeEqualTo "a"
        result2 shouldBeEqualTo "b"
    }

    @Test
    fun `runIfLeader - action 예외 발생 시 예외가 호출자에게 전파된다`() {
        assertThrows<RuntimeException> {
            election.runIfLeader(randomLockName()) {
                throw RuntimeException("테스트 예외")
            }
        }
    }

    @Test
    fun `runIfLeader - action 예외 후에도 락이 해제되어 다음 호출이 성공한다`() {
        val lockName = randomLockName()
        runCatching {
            election.runIfLeader(lockName) { throw RuntimeException("실패") }
        }

        // 락이 해제된 상태여야 다음 호출이 정상 실행된다
        val result = election.runIfLeader(lockName) { "복구 성공" }
        result shouldBeEqualTo "복구 성공"
    }

    @Test
    fun `runIfLeader - 동일 스레드에서 동일 lockName 으로 중첩 호출(재진입)이 가능하다`() {
        val lockName = randomLockName()
        val result = election.runIfLeader(lockName) {
            // ReentrantLock 은 동일 스레드에서 재진입이 가능하다
            election.runIfLeader(lockName) { "재진입 성공" }
        }
        result shouldBeEqualTo "재진입 성공"
    }

    @Test
    fun `runIfLeader - 멀티스레드 동시 실행 시 직렬 처리를 보장한다`() {
        val lockName = randomLockName()
        val counter = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        MultithreadingTester()
            .workers(numThreads)
            .rounds(roundsPerThread)
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "작업 1 실행. counter=${counter.get()}" }
                    Thread.sleep(Random.nextLong(1, 5))
                    counter.incrementAndGet()
                }
            }
            .add {
                election.runIfLeader(lockName) {
                    log.debug { "작업 2 실행. counter=${counter.get()}" }
                    Thread.sleep(Random.nextLong(1, 5))
                    counter.incrementAndGet()
                }
            }
            .run()

        // workers * rounds = 8 * 4 = 32 태스크, block 이 2개이므로 각 block 은 16번 실행
        counter.get() shouldBeEqualTo numThreads * roundsPerThread
    }

    @Test
    fun `runAsyncIfLeader - 리더로 선출되어 비동기 action 을 실행하고 결과를 반환한다`() {
        val result = election.runAsyncIfLeader(randomLockName()) {
            CompletableFuture.completedFuture("async-ok")
        }.join()

        result shouldBeEqualTo "async-ok"
    }

    @Test
    fun `runAsyncIfLeader - action future 실패 시 CompletionException 이 전파된다`() {
        assertThrows<CompletionException> {
            election.runAsyncIfLeader(randomLockName()) {
                CompletableFuture.failedFuture<String>(IllegalStateException("비동기 실패"))
            }.join()
        }
    }

    @Test
    fun `runAsyncIfLeader - action future 실패 후에도 락이 해제되어 다음 호출이 성공한다`() {
        val lockName = randomLockName()
        runCatching {
            election.runAsyncIfLeader(lockName) {
                CompletableFuture.failedFuture<Int>(RuntimeException("실패"))
            }.join()
        }

        val result = election.runAsyncIfLeader(lockName) {
            CompletableFuture.completedFuture(42)
        }.join()

        result shouldBeEqualTo 42
    }

    @Test
    fun `runAsyncIfLeader - 멀티스레드 동시 실행 시 직렬 처리를 보장한다`() {
        val lockName = randomLockName()
        val counter = AtomicInteger(0)
        val numThreads = 8
        val roundsPerThread = 4

        MultithreadingTester()
            .workers(numThreads)
            .rounds(roundsPerThread)
            .add {
                election.runAsyncIfLeader(lockName) {
                    CompletableFuture.supplyAsync {
                        log.debug { "비동기 작업 1 실행. counter=${counter.get()}" }
                        Thread.sleep(Random.nextLong(1, 5))
                        counter.incrementAndGet()
                    }
                }.join()
            }
            .add {
                election.runAsyncIfLeader(lockName) {
                    CompletableFuture.supplyAsync {
                        log.debug { "비동기 작업 2 실행. counter=${counter.get()}" }
                        Thread.sleep(Random.nextLong(1, 5))
                        counter.incrementAndGet()
                    }
                }.join()
            }
            .run()

        counter.get() shouldBeEqualTo numThreads * roundsPerThread
    }
}
