package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class LettuceSuspendLeaderGroupElectionTest: AbstractLettuceTest() {

    companion object: KLogging()

    private val maxLeaders = 3
    private val options = LeaderGroupElectionOptions(maxLeaders, Duration.ofSeconds(5), Duration.ofSeconds(10))

    private lateinit var election: LettuceLeaderGroupElection
    private lateinit var suspendElection: LettuceSuspendLeaderGroupElection
    private lateinit var lockName: String

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        suspendElection = LettuceSuspendLeaderGroupElection(connection, options)
        lockName = randomName()
    }

    @AfterEach
    fun teardown() {
        // 세마포어 키 정리
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        connection.sync().del(lockName)
    }

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
        val el = connection.leaderGroupElection(options)
        el.shouldNotBeNull()
        val result = el.runIfLeader(lockName) { "ext" }
        result shouldBeEqualTo "ext"
    }

    @Test
    fun `확장 함수로 LettuceSuspendLeaderGroupElection 생성`() = runSuspendIO {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = connection.suspendLeaderGroupElection(options)
        el.shouldNotBeNull()
        val result = el.runIfLeader(lockName) { "ext-suspend" }
        result shouldBeEqualTo "ext-suspend"
    }
}
