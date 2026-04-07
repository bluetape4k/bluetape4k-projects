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

class LocalAsyncLeaderElectionTest {

    companion object: KLogging()

    private val election = LocalAsyncLeaderElection()

    private fun randomLockName() = "lock-${UUID.randomUUID()}"

    @Test
    fun `runAsyncIfLeader - 리더로 선출되어 비동기 action 을 실행하고 결과를 반환한다`() {
        val result = election.runAsyncIfLeader(randomLockName()) {
            CompletableFuture.completedFuture("async-ok")
        }.join()

        result shouldBeEqualTo "async-ok"
    }

    @Test
    fun `runAsyncIfLeader - 서로 다른 lockName 은 독립적으로 실행된다`() {
        val future1 = election.runAsyncIfLeader(randomLockName()) {
            CompletableFuture.completedFuture("a")
        }
        val future2 = election.runAsyncIfLeader(randomLockName()) {
            CompletableFuture.completedFuture("b")
        }

        future1.join() shouldBeEqualTo "a"
        future2.join() shouldBeEqualTo "b"
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

        // 락이 해제된 상태여야 다음 호출이 정상 실행된다
        val result = election.runAsyncIfLeader(lockName) {
            CompletableFuture.completedFuture(99)
        }.join()

        result shouldBeEqualTo 99
    }

    @Test
    fun `runAsyncIfLeader - action 내부 예외 발생 시 CompletionException 이 전파된다`() {
        assertThrows<CompletionException> {
            election.runAsyncIfLeader(randomLockName()) {
                CompletableFuture.supplyAsync<Int> { throw RuntimeException("action 내부 예외") }
            }.join()
        }
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
