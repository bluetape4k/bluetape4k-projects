package io.bluetape4k.javers.persistence.kafka.repository

import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.springframework.kafka.core.KafkaTemplate

/**
 * Javers 의 Audit 정보를 Kafka 로 발행합니다.
 * 저장소로 쓰이는 것이 아니므로, 저장 역할만 가능하고, 조회 기능은 없습니다.
 *
 * @property kafkaOperations [KafkaTemplate] 인스턴스
 */
class KafkaCdoSnapshotRepository(
    private val kafkaOperations: KafkaTemplate<String, String>,
): AbstractCdoSnapshotRepository<String>(JaversCodecs.String) {

    companion object: KLogging()

    override fun getKeys(): List<String> = emptyList()

    override fun contains(globalIdValue: String): Boolean = false

    override fun getSeq(commitId: CommitId): Long = 0L

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        // Nothing to do.
    }

    override fun getSnapshotSize(globalIdValue: String): Int = 0

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        try {
            val key = snapshot.globalId.value()
            val value = encode(snapshot)
            log.trace { "Produce snapshot. key=$key, value=$value" }
            kafkaOperations.sendDefault(key, value)
        } catch (e: Throwable) {
            log.error(e) { "Fail to procude snapshot. key=${snapshot.globalId.value()}" }
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> = emptyList()
}
