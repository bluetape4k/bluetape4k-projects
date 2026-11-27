package io.bluetape4k.kafka.test.conformance

import java.time.Duration
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class KafkaRecoveryConformanceFixtureTest {
    private val fixture = KafkaRecoveryConformanceFixture(Duration.ofSeconds(15))
    private val ids = setOf("event-1", "event-2")

    @Test
    fun `transport recovery accepts only transport evidence`() {
        fixture.assertTransportRecovery(observation(KafkaRecoveryPath.TRANSPORT_INTERRUPTION))
    }

    @Test
    fun `leader and coordinator failover keep their evidence boundaries`() {
        fixture.assertBrokerLeaderFailover(observation(KafkaRecoveryPath.BROKER_LEADER_FAILOVER))
        fixture.assertBrokerCoordinatorFailover(observation(KafkaRecoveryPath.BROKER_COORDINATOR_FAILOVER))
    }

    @Test
    fun `dedup boundary requires replay and unique effects`() {
        fixture.assertDedupBoundary(ids, listOf("event-1", "event-2", "event-1"), ids)
    }

    @Test
    fun `deadline and path mismatch are rejected`() {
        assertFailsWith<IllegalStateException> {
            fixture.assertTransportRecovery(observation(KafkaRecoveryPath.BROKER_LEADER_FAILOVER))
        }
        assertFailsWith<IllegalStateException> {
            fixture.assertBrokerLeaderFailover(
                observation(
                    KafkaRecoveryPath.BROKER_LEADER_FAILOVER,
                    recoveryElapsed = Duration.ofSeconds(16),
                ),
            )
        }
    }

    private fun observation(
        path: KafkaRecoveryPath,
        recoveryElapsed: Duration = Duration.ofSeconds(1),
    ): KafkaRecoveryObservation = KafkaRecoveryObservation(
        path = path,
        logicalEventIds = ids,
        deliveredEventIds = ids.toList(),
        appliedEventIds = ids,
        conflictCount = 0,
        recoveryElapsed = recoveryElapsed,
        recovered = true,
        transportInterrupted = path == KafkaRecoveryPath.TRANSPORT_INTERRUPTION,
        leaderChanged = path == KafkaRecoveryPath.BROKER_LEADER_FAILOVER,
        coordinatorChanged = path == KafkaRecoveryPath.BROKER_COORDINATOR_FAILOVER,
        replacementReady = path != KafkaRecoveryPath.TRANSPORT_INTERRUPTION,
        isrRestored = path != KafkaRecoveryPath.TRANSPORT_INTERRUPTION,
    )
}
