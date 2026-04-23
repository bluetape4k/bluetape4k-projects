package io.bluetape4k.leader

import io.bluetape4k.leader.local.LocalAsyncLeaderGroupElection
import io.bluetape4k.leader.local.LocalLeaderGroupElection
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * [LeaderGroupElectionState] 인터페이스 계약을 검증하는 테스트입니다.
 *
 * [LocalLeaderGroupElection]과 [LocalAsyncLeaderGroupElection] 구현체를 통해
 * [LeaderGroupElectionState]의 상태 조회 메서드([activeCount], [availableSlots], [state])를 검증합니다.
 */
class LeaderGroupElectionStateTest {

    companion object: KLogging()

    private val maxLeaders = 3
    private val options = LeaderGroupElectionOptions(maxLeaders)

    private fun randomLockName() = "lock-${UUID.randomUUID()}"

    // ── LocalLeaderGroupElection 을 통한 LeaderGroupElectionState 계약 검증 ──

    @Test
    fun `activeCount - 초기 상태에서 0을 반환한다 (sync 구현체)`() {
        val election: LeaderGroupElectionState = LocalLeaderGroupElection(options)
        val lockName = randomLockName()
        election.activeCount(lockName) shouldBeEqualTo 0
    }

    @Test
    fun `availableSlots - 초기 상태에서 maxLeaders 를 반환한다 (sync 구현체)`() {
        val election: LeaderGroupElectionState = LocalLeaderGroupElection(options)
        val lockName = randomLockName()
        election.availableSlots(lockName) shouldBeEqualTo maxLeaders
    }

    @Test
    fun `state - 초기 상태 스냅샷이 정확하다 (sync 구현체)`() {
        val election: LeaderGroupElectionState = LocalLeaderGroupElection(options)
        val lockName = randomLockName()
        val state = election.state(lockName)

        state.lockName shouldBeEqualTo lockName
        state.maxLeaders shouldBeEqualTo maxLeaders
        state.activeCount shouldBeEqualTo 0
        state.availableSlots shouldBeEqualTo maxLeaders
        state.isEmpty.shouldBeTrue()
        state.isFull.shouldBeFalse()
    }

    @Test
    fun `state - 슬롯 점유 중 activeCount 가 증가한다 (sync 구현체)`() {
        val election = LocalLeaderGroupElection(options)
        val lockName = randomLockName()
        val startLatch = CountDownLatch(2)
        val holdLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        repeat(2) {
            executor.submit {
                election.runIfLeader(lockName) {
                    startLatch.countDown()
                    holdLatch.await()
                }
            }
        }
        startLatch.await(2, TimeUnit.SECONDS)

        election.activeCount(lockName) shouldBeEqualTo 2
        election.availableSlots(lockName) shouldBeEqualTo maxLeaders - 2
        election.state(lockName).isEmpty.shouldBeFalse()

        holdLatch.countDown()
        executor.shutdown()
        executor.awaitTermination(3, TimeUnit.SECONDS)
    }

    // ── LocalAsyncLeaderGroupElection 을 통한 LeaderGroupElectionState 계약 검증 ──

    @Test
    fun `activeCount - 초기 상태에서 0을 반환한다 (async 구현체)`() {
        val election: LeaderGroupElectionState = LocalAsyncLeaderGroupElection(options)
        val lockName = randomLockName()
        election.activeCount(lockName) shouldBeEqualTo 0
    }

    @Test
    fun `availableSlots - 초기 상태에서 maxLeaders 를 반환한다 (async 구현체)`() {
        val election: LeaderGroupElectionState = LocalAsyncLeaderGroupElection(options)
        val lockName = randomLockName()
        election.availableSlots(lockName) shouldBeEqualTo maxLeaders
    }

    @Test
    fun `state - maxLeaders 프로퍼티가 올바르다`() {
        val election: LeaderGroupElectionState = LocalLeaderGroupElection(options)
        election.maxLeaders shouldBeEqualTo maxLeaders
    }
}
