package io.bluetape4k.leader

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

class LeaderElectionOptionsTest {

    companion object: KLogging()

    @Test
    fun `기본 옵션 값이 올바르다`() {
        val options = LeaderElectionOptions.Default
        options.waitTime shouldBeEqualTo Duration.ofSeconds(5)
        options.leaseTime shouldBeEqualTo Duration.ofSeconds(60)
    }

    @Test
    fun `커스텀 옵션 값을 설정할 수 있다`() {
        val options = LeaderElectionOptions(
            waitTime = Duration.ofSeconds(10),
            leaseTime = Duration.ofSeconds(120),
        )
        options.waitTime shouldBeEqualTo Duration.ofSeconds(10)
        options.leaseTime shouldBeEqualTo Duration.ofSeconds(120)
    }

    @Test
    fun `LeaderGroupElectionOptions 기본 값이 올바르다`() {
        val options = LeaderGroupElectionOptions.Default
        options.maxLeaders shouldBeEqualTo 2
        options.waitTime shouldBeEqualTo Duration.ofSeconds(5)
        options.leaseTime shouldBeEqualTo Duration.ofSeconds(60)
    }

    @Test
    fun `LeaderGroupElectionOptions maxLeaders 가 0 이하면 예외가 발생한다`() {
        assertThrows<IllegalArgumentException> {
            io.bluetape4k.leader.local.LocalLeaderGroupElection(
                LeaderGroupElectionOptions(maxLeaders = 0)
            )
        }
        assertThrows<IllegalArgumentException> {
            io.bluetape4k.leader.local.LocalLeaderGroupElection(
                LeaderGroupElectionOptions(maxLeaders = -1)
            )
        }
    }

    @Test
    fun `blank lockName 으로 호출 시 예외가 발생한다`() {
        val election = io.bluetape4k.leader.local.LocalLeaderElection()
        assertThrows<IllegalArgumentException> {
            election.runIfLeader("") { "should fail" }
        }
        assertThrows<IllegalArgumentException> {
            election.runIfLeader("   ") { "should fail" }
        }
    }
}
