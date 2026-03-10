package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.leader.coroutines.LettuceSuspendLeaderGroupElection
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceLeaderGroupElectionTest : AbstractLettuceTest() {

    private val maxLeaders = 3
    private val options = LettuceLeaderElectionOptions(waitTime = 5.seconds, leaseTime = 10.seconds)

    private lateinit var election: LettuceLeaderGroupElection
    private lateinit var suspendElection: LettuceSuspendLeaderGroupElection
    private lateinit var lockName: String

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        election = LettuceLeaderGroupElection(connection, maxLeaders, options)
        suspendElection = LettuceSuspendLeaderGroupElection(connection, maxLeaders, options)
        lockName = randomName()
    }

    @AfterEach
    fun teardown() {
        // 세마포어 키 정리
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        connection.sync().del(lockName)
    }

    // =========================================================================
    // 상태 조회
    // =========================================================================

    @Test
    fun `maxLeaders 값 확인`() {
        election.maxLeaders shouldBeEqualTo maxLeaders
    }

    @Test
    fun `초기 상태 - availableSlots = maxLeaders`() {
        val slots = election.availableSlots(lockName)
        slots shouldBeEqualTo maxLeaders
    }

    @Test
    fun `초기 상태 - activeCount = 0`() {
        val active = election.activeCount(lockName)
        active shouldBeEqualTo 0
    }

    @Test
    fun `state 조회`() {
        val state = election.state(lockName)
        state.lockName shouldBeEqualTo lockName
        state.maxLeaders shouldBeEqualTo maxLeaders
        state.activeCount shouldBeEqualTo 0
        state.isEmpty.shouldBeTrue()
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
        val r1 = election.runIfLeader(lockName) { 1 }
        val r2 = election.runIfLeader(lockName) { 2 }
        val r3 = election.runIfLeader(lockName) { 3 }
        r1 shouldBeEqualTo 1
        r2 shouldBeEqualTo 2
        r3 shouldBeEqualTo 3
    }

    @Test
    fun `action 예외 발생 시 슬롯 반환 후 재선출 가능`() {
        try {
            election.runIfLeader(lockName) { throw RuntimeException("오류") }
        } catch (_: RuntimeException) {
        }
        // 슬롯이 반환되어 다시 선출 가능
        val result = election.runIfLeader(lockName) { "recovered" }
        result shouldBeEqualTo "recovered"
    }

    // =========================================================================
    // 비동기 API
    // =========================================================================

    @Test
    fun `비동기 리더 선출 성공`() {
        val result = election.runAsyncIfLeader(lockName) {
            CompletableFuture.completedFuture("async-done")
        }.get()
        result shouldBeEqualTo "async-done"
    }

    @Test
    fun `비동기 복수 리더 동시 실행`() {
        val counter = AtomicInteger(0)
        val futures = List(maxLeaders) { i ->
            election.runAsyncIfLeader(lockName) {
                CompletableFuture.supplyAsync {
                    Thread.sleep(50)
                    counter.incrementAndGet()
                    "result-$i"
                }
            }
        }
        CompletableFuture.allOf(*futures.toTypedArray()).get()
        counter.get() shouldBeEqualTo maxLeaders
    }

    // =========================================================================
    // 코루틴 API
    // =========================================================================

    @Test
    fun `코루틴 리더 선출 성공`() = runSuspendIO {
        val result = suspendElection.runIfLeader(lockName) { "suspend-done" }
        result shouldBeEqualTo "suspend-done"
    }

    @Test
    fun `코루틴 복수 리더 동시 실행`() = runSuspendIO {
        val counter = AtomicInteger(0)
        val jobs = List(maxLeaders) {
            async {
                suspendElection.runIfLeader(lockName) {
                    counter.incrementAndGet()
                }
            }
        }
        jobs.awaitAll()
        counter.get() shouldBeEqualTo maxLeaders
    }

    @Test
    fun `코루틴 상태 조회`() = runSuspendIO {
        val state = suspendElection.state(lockName)
        state.maxLeaders shouldBeEqualTo maxLeaders
        state.activeCount shouldBeEqualTo 0
    }

    // =========================================================================
    // 확장 함수
    // =========================================================================

    @Test
    fun `확장 함수로 LettuceLeaderGroupElection 생성`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = connection.leaderGroupElection(maxLeaders, options)
        el.shouldNotBeNull()
        val result = el.runIfLeader(lockName) { "ext" }
        result shouldBeEqualTo "ext"
    }

    @Test
    fun `확장 함수로 LettuceSuspendLeaderGroupElection 생성`() = runSuspendIO {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = connection.suspendLeaderGroupElection(maxLeaders, options)
        el.shouldNotBeNull()
        val result = el.runIfLeader(lockName) { "ext-suspend" }
        result shouldBeEqualTo "ext-suspend"
    }
}
