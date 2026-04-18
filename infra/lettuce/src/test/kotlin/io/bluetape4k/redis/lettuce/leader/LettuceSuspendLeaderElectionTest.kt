package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.leader.LeaderElectionOptions
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

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

    // =========================================================================
    // SuspendedJobTester 동시성 테스트
    // =========================================================================

    @Test
    fun `SuspendedJobTester - 코루틴 동시 리더 선출 상호 배제 검증`() = runSuspendIO {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = LettuceSuspendLeaderElection(connection, options)
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val executed = AtomicInteger(0)

        SuspendedJobTester()
            .workers(5)
            .rounds(3)
            .add {
                el.runIfLeader(lockName) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    delay(10.milliseconds)
                    concurrent.decrementAndGet()
                    executed.incrementAndGet()
                }
            }
            .run()

        maxConcurrent.get() shouldBeEqualTo 1
        executed.get() shouldBeGreaterOrEqualTo 1
    }

    @Test
    fun `SuspendedJobTester - 코루틴 리더 선출 결과 정합성`() = runSuspendIO {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val el = LettuceSuspendLeaderElection(connection, options)
        val counter = AtomicInteger(0)

        SuspendedJobTester()
            .workers(4)
            .rounds(3)
            .add {
                el.runIfLeader(lockName) {
                    counter.incrementAndGet()
                }
            }
            .run()

        counter.get() shouldBeGreaterOrEqualTo 1
    }
}
