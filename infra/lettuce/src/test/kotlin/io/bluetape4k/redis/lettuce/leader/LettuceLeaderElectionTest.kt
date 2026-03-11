package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.leader.LeaderElectionOptions
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceLeaderElectionTest: AbstractLettuceTest() {

    private val options = LeaderElectionOptions(waitTime = Duration.ofSeconds(2), Duration.ofSeconds(10))

    private lateinit var election: LettuceLeaderElection
    private lateinit var suspendElection: LettuceSuspendLeaderElection
    private lateinit var lockName: String

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        election = LettuceLeaderElection(connection, options)
        lockName = randomName()
    }

    // =========================================================================
    // 동기 API
    // =========================================================================

    @Test
    fun `리더 선출 성공 시 action 실행`() {
        val result = election.runIfLeader(lockName) { "done" }
        result shouldBeEqualTo "done"
    }

    @Test
    fun `리더 선출 - 여러 번 순차 실행 가능`() {
        val result1 = election.runIfLeader(lockName) { 1 }
        val result2 = election.runIfLeader(lockName) { 2 }
        result1 shouldBeEqualTo 1
        result2 shouldBeEqualTo 2
    }

    @Test
    fun `리더 선출 - action 예외 발생 시 예외 전파`() {
        var threw = false
        try {
            election.runIfLeader(lockName) { throw RuntimeException("오류") }
        } catch (e: RuntimeException) {
            threw = true
            e.message shouldBeEqualTo "오류"
        }
        threw.shouldBeTrue()
    }

    @Test
    fun `리더 선출 - action 예외 후 락 해제되어 재선출 가능`() {
        try {
            election.runIfLeader(lockName) { throw RuntimeException("오류") }
        } catch (_: RuntimeException) {
        }
        // 예외 후에도 다시 선출 가능
        val result = election.runIfLeader(lockName) { "recovered" }
        result shouldBeEqualTo "recovered"
    }

    // =========================================================================
    // 비동기 API
    // =========================================================================

    @Test
    fun `비동기 리더 선출 성공`() {
        val future = election.runAsyncIfLeader(lockName) {
            java.util.concurrent.CompletableFuture.completedFuture("async-done")
        }
        future.get() shouldBeEqualTo "async-done"
    }

    @Test
    fun `비동기 리더 선출 - 여러 번 순차 실행 가능`() {
        val r1 = election.runAsyncIfLeader(lockName) {
            java.util.concurrent.CompletableFuture.completedFuture(1)
        }.get()
        val r2 = election.runAsyncIfLeader(lockName) {
            java.util.concurrent.CompletableFuture.completedFuture(2)
        }.get()
        r1 shouldBeEqualTo 1
        r2 shouldBeEqualTo 2
    }

    // =========================================================================
    // MultithreadingTester 동시성 테스트
    // =========================================================================

    @Test
    fun `MultithreadingTester - 동시 리더 선출 상호 배제 검증`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = LettuceLeaderElection(connection, options)
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val executed = AtomicInteger(0)

        MultithreadingTester()
            .workers(5)
            .rounds(3)
            .add {
                el.runIfLeader(lockName) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    Thread.sleep(10)
                    concurrent.decrementAndGet()
                    executed.incrementAndGet()
                }
            }
            .run()

        // 리더 선출은 상호 배제이므로 동시에 1개만 실행되어야 함
        maxConcurrent.get() shouldBeEqualTo 1
        executed.get() shouldBeGreaterOrEqualTo 1
    }

    @Test
    fun `MultithreadingTester - 동시 비동기 리더 선출 안정성`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = LettuceLeaderElection(connection, options)
        val executed = AtomicInteger(0)

        MultithreadingTester()
            .workers(4)
            .rounds(3)
            .add {
                el.runAsyncIfLeader(lockName) {
                    java.util.concurrent.CompletableFuture.supplyAsync {
                        executed.incrementAndGet()
                    }
                }.get()
            }
            .run()

        executed.get() shouldBeGreaterOrEqualTo 1
    }

    // =========================================================================
    // StructuredTaskScopeTester 동시성 테스트
    // =========================================================================

    @Test
    fun `StructuredTaskScopeTester - 동시 리더 선출 상호 배제 검증`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = LettuceLeaderElection(connection, options)
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val executed = AtomicInteger(0)

        StructuredTaskScopeTester()
            .rounds(10)
            .add {
                el.runIfLeader(lockName) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    Thread.sleep(10)
                    concurrent.decrementAndGet()
                    executed.incrementAndGet()
                }
            }
            .run()

        maxConcurrent.get() shouldBeEqualTo 1
        executed.get() shouldBeGreaterOrEqualTo 1
    }
}
