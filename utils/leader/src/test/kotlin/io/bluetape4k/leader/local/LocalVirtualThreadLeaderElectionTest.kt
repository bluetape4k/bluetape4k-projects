package io.bluetape4k.leader.local

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class LocalVirtualThreadLeaderElectionTest {

    companion object : KLogging()

    private val election = LocalVirtualThreadLeaderElection()

    private fun randomLockName() = "lock-${UUID.randomUUID()}"

    @Test
    fun `runAsyncIfLeader - 리더로 선출되어 action 을 실행하고 결과를 반환한다`() {
        val result = election.runAsyncIfLeader(randomLockName()) { "hello" }.await()
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `runAsyncIfLeader - 서로 다른 lockName 은 독립적으로 실행된다`() {
        val f1 = election.runAsyncIfLeader(randomLockName()) { "a" }
        val f2 = election.runAsyncIfLeader(randomLockName()) { "b" }

        f1.await() shouldBeEqualTo "a"
        f2.await() shouldBeEqualTo "b"
    }

    @Test
    fun `runAsyncIfLeader - action 예외 발생 시 await 호출 시 예외가 전파된다`() {
        val result = runCatching {
            election.runAsyncIfLeader(randomLockName()) {
                throw RuntimeException("테스트 예외")
            }.await()
        }
        result.isFailure.shouldBeTrue()
    }

    @Test
    fun `runAsyncIfLeader - action 예외 후에도 락이 해제되어 다음 호출이 성공한다`() {
        val lockName = randomLockName()
        runCatching {
            election.runAsyncIfLeader(lockName) {
                throw RuntimeException("실패")
            }.await()
        }

        // 락이 해제된 상태여야 다음 호출이 정상 실행된다
        val result = election.runAsyncIfLeader(lockName) { "복구 성공" }.await()
        result shouldBeEqualTo "복구 성공"
    }

    @Test
    fun `runAsyncIfLeader - toCompletableFuture 로 변환하여 결과를 소비할 수 있다`() {
        val result = election
            .runAsyncIfLeader(randomLockName()) { 42 }
            .toCompletableFuture()
            .join()

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
                    log.debug { "Virtual Thread 작업 1 실행. counter=${counter.get()}" }
                    Thread.sleep(Random.nextLong(1, 5))
                    counter.incrementAndGet()
                }.await()
            }
            .add {
                election.runAsyncIfLeader(lockName) {
                    log.debug { "Virtual Thread 작업 2 실행. counter=${counter.get()}" }
                    Thread.sleep(Random.nextLong(1, 5))
                    counter.incrementAndGet()
                }.await()
            }
            .run()

        counter.get() shouldBeEqualTo numThreads * roundsPerThread
    }
}
