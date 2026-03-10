package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.leader.coroutines.LettuceSuspendLeaderElection
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceLeaderElectionTest : AbstractLettuceTest() {

    private val options = LettuceLeaderElectionOptions(waitTime = 2.seconds, leaseTime = 10.seconds)

    private lateinit var election: LettuceLeaderElection
    private lateinit var suspendElection: LettuceSuspendLeaderElection
    private lateinit var lockName: String

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        election = LettuceLeaderElection(connection, options)
        suspendElection = LettuceSuspendLeaderElection(connection, options)
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
    // 코루틴 API
    // =========================================================================

    @Test
    fun `코루틴 리더 선출 성공`() = runSuspendIO {
        val result = suspendElection.runIfLeader(lockName) { "suspend-done" }
        result shouldBeEqualTo "suspend-done"
    }

    @Test
    fun `코루틴 리더 선출 - 여러 번 순차 실행 가능`() = runSuspendIO {
        val r1 = suspendElection.runIfLeader(lockName) { "first" }
        val r2 = suspendElection.runIfLeader(lockName) { "second" }
        r1 shouldBeEqualTo "first"
        r2 shouldBeEqualTo "second"
    }

    @Test
    fun `코루틴 리더 선출 - action 예외 후 재선출 가능`() = runSuspendIO {
        try {
            suspendElection.runIfLeader(lockName) { throw RuntimeException("suspend 오류") }
        } catch (_: RuntimeException) {
        }
        val result = suspendElection.runIfLeader(lockName) { "recovered" }
        result shouldBeEqualTo "recovered"
    }

    // =========================================================================
    // 확장 함수
    // =========================================================================

    @Test
    fun `확장 함수로 LettuceLeaderElection 생성`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = connection.leaderElection(options)
        el.shouldNotBeNull()
        val result = el.runIfLeader(lockName) { "ext" }
        result shouldBeEqualTo "ext"
    }

    @Test
    fun `확장 함수로 LettuceSuspendLeaderElection 생성`() = runSuspendIO {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = connection.suspendLeaderElection(options)
        el.shouldNotBeNull()
        val result = el.runIfLeader(lockName) { "ext-suspend" }
        result shouldBeEqualTo "ext-suspend"
    }
}
