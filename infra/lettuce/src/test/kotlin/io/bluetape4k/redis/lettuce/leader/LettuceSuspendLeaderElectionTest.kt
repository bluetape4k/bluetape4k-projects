package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.leader.LeaderElectionOptions
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

class LettuceSuspendLeaderElectionTest: AbstractLettuceTest() {

    companion object: KLogging()

    private val options = LeaderElectionOptions(waitTime = Duration.ofSeconds(2), Duration.ofSeconds(10))

    private lateinit var suspendElection: LettuceSuspendLeaderElection
    private lateinit var lockName: String

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        suspendElection = LettuceSuspendLeaderElection(connection, options)
        lockName = randomName()
    }

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
