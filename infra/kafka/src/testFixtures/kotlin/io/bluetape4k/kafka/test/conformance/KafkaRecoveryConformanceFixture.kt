package io.bluetape4k.kafka.test.conformance

import java.time.Duration

/**
 * 독립적인 Kafka 예제가 공유하는 black-box recovery contract입니다.
 *
 * 이 fixture는 observation만 받으며 Kafka client를 만들거나 broker를 소유하지 않고
 * production messaging abstraction도 노출하지 않습니다.
 */
class KafkaRecoveryConformanceFixture(
    private val recoveryDeadline: Duration,
) {
    init {
        require(!recoveryDeadline.isNegative && !recoveryDeadline.isZero) {
            "recoveryDeadline must be positive"
        }
    }

    fun assertTransportRecovery(observation: KafkaRecoveryObservation) {
        check(observation.path == KafkaRecoveryPath.TRANSPORT_INTERRUPTION) {
            "transport recovery observation has the wrong path"
        }
        check(observation.transportInterrupted) { "transport interruption was not observed" }
        check(!observation.leaderChanged) { "transport recovery must not claim broker leader movement" }
        check(!observation.coordinatorChanged) { "transport recovery must not claim coordinator movement" }
        assertCommon(observation)
    }

    fun assertBrokerLeaderFailover(observation: KafkaRecoveryObservation) {
        check(observation.path == KafkaRecoveryPath.BROKER_LEADER_FAILOVER) {
            "broker failover observation has the wrong path"
        }
        check(observation.leaderChanged) { "broker leader failover must move the leader" }
        check(observation.replacementReady) { "broker replacement was not ready" }
        check(observation.isrRestored) { "broker ISR was not restored" }
        check(!observation.transportInterrupted) {
            "broker failover evidence must not be labelled as transport interruption"
        }
        assertCommon(observation)
    }

    fun assertBrokerCoordinatorFailover(observation: KafkaRecoveryObservation) {
        check(observation.path == KafkaRecoveryPath.BROKER_COORDINATOR_FAILOVER) {
            "coordinator failover observation has the wrong path"
        }
        check(observation.coordinatorChanged) { "broker coordinator failover must move the coordinator" }
        check(!observation.leaderChanged) { "coordinator failover must keep the selected data leader stable" }
        check(observation.replacementReady) { "coordinator replacement was not ready" }
        check(observation.isrRestored) { "coordinator scenario ISR was not restored" }
        check(!observation.transportInterrupted) {
            "coordinator failover evidence must not be labelled as transport interruption"
        }
        assertCommon(observation)
    }

    /** Replay로 delivery 수가 늘어도 logical effect는 유일해야 함을 검증합니다. */
    fun assertDedupBoundary(
        logicalEventIds: Set<String>,
        deliveredEventIds: List<String>,
        appliedEventIds: Set<String>,
    ) {
        require(logicalEventIds.isNotEmpty()) { "logicalEventIds must not be empty" }
        check(deliveredEventIds.toSet() == logicalEventIds) {
            "delivery must contain exactly the logical identities, allowing replay duplicates"
        }
        check(appliedEventIds == logicalEventIds) { "applied effects must be unique by logical identity" }
        check(deliveredEventIds.size >= appliedEventIds.size) {
            "applied effects cannot exceed delivered logical identities"
        }
        check(deliveredEventIds.size > appliedEventIds.size) {
            "dedup boundary requires an intentional replay/duplicate delivery"
        }
    }

    private fun assertCommon(observation: KafkaRecoveryObservation) {
        require(observation.logicalEventIds.isNotEmpty()) { "logicalEventIds must not be empty" }
        check(observation.recovered) { "recovery did not reach a terminal state" }
        check(observation.recoveryElapsed <= recoveryDeadline) {
            "recovery exceeded the documented deadline"
        }
        check(observation.appliedEventIds == observation.logicalEventIds) {
            "applied logical identities must equal the expected identity set"
        }
        check(observation.deliveredEventIds.toSet() == observation.logicalEventIds) {
            "delivery must cover the expected logical identity set"
        }
        check(observation.deliveredEventIds.size >= observation.appliedEventIds.size) {
            "delivery count cannot be lower than unique applied count"
        }
        check(observation.conflictCount == 0) { "logical identity fingerprint conflict was observed" }
    }
}

enum class KafkaRecoveryPath {
    TRANSPORT_INTERRUPTION,
    BROKER_LEADER_FAILOVER,
    BROKER_COORDINATOR_FAILOVER,
}

data class KafkaRecoveryObservation(
    val path: KafkaRecoveryPath,
    val logicalEventIds: Set<String>,
    val deliveredEventIds: List<String>,
    val appliedEventIds: Set<String>,
    val conflictCount: Int,
    val recoveryElapsed: Duration,
    val recovered: Boolean,
    val transportInterrupted: Boolean,
    val leaderChanged: Boolean,
    val coordinatorChanged: Boolean,
    val replacementReady: Boolean,
    val isrRestored: Boolean,
) {
    init {
        require(conflictCount >= 0) { "conflictCount must not be negative" }
        require(!recoveryElapsed.isNegative) { "recoveryElapsed must not be negative" }
    }
}
