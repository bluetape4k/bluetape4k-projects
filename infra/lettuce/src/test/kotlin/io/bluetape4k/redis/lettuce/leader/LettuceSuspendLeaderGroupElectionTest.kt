package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeInRange
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

    // =========================================================================
    // SuspendedJobTester 동시성 테스트
    // =========================================================================

    @Test
    fun `SuspendedJobTester - 코루틴 동시 리더 그룹 선출 maxLeaders 제한 검증`() = runSuspendIO {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = LettuceSuspendLeaderGroupElection(connection, options)
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val executed = AtomicInteger(0)

        SuspendedJobTester()
            .workers(maxLeaders * 2)
            .rounds(maxLeaders * 3)
            .add {
                el.runIfLeader(lockName) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    delay(20)
                    concurrent.decrementAndGet()
                    executed.incrementAndGet()
                }
            }
            .run()

        // 동시 실행 수는 maxLeaders 이하여야 함
        maxConcurrent.get() shouldBeInRange 1..maxLeaders
        executed.get() shouldBeGreaterOrEqualTo 1
    }

    @Test
    fun `SuspendedJobTester - 코루틴 리더 그룹 총 실행 횟수 검증`() = runSuspendIO {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = LettuceSuspendLeaderGroupElection(connection, options)
        val executed = AtomicInteger(0)
        val rounds = 10

        SuspendedJobTester()
            .workers(maxLeaders)
            .rounds(rounds)
            .add {
                el.runIfLeader(lockName) {
                    executed.incrementAndGet()
                }
            }
            .run()

        // SuspendedJobTester: 총 실행 횟수 = rounds (workers는 병렬도만 제어)
        executed.get() shouldBeEqualTo rounds
    }

}
