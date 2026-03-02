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
 * JaVers [CdoSnapshot]을 Kafka 토픽으로 발행하는 쓰기 전용 저장소.
 *
 * ## 동작/계약
 * - [saveSnapshot]에서 [KafkaTemplate.sendDefault]로 GlobalId를 key, 인코딩된 스냅샷을 value로 발행한다
 * - 발행 실패 시 예외를 로깅하고 삼킨다 (조용한 실패)
 * - 조회 메서드([loadSnapshots], [getKeys] 등)는 항상 빈 컬렉션/0을 반환한다
 * - 코덱은 [JaversCodecs.String] (비압축 JSON 문자열)을 사용한다
 *
 * ```kotlin
 * val repo = KafkaCdoSnapshotRepository(kafkaTemplate)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * // javers.commit("author", entity) → Kafka 토픽으로 스냅샷 발행
 * ```
 *
 * @property kafkaOperations [KafkaTemplate] 인스턴스
 */
class KafkaCdoSnapshotRepository(
    private val kafkaOperations: KafkaTemplate<String, String>,
): AbstractCdoSnapshotRepository<String>(JaversCodecs.String) {

    companion object: KLogging()

    override fun getKeys(): Set<String> = emptySet()

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
            kafkaOperations.sendDefault(key, value).get()
        } catch (e: Throwable) {
            log.error(e) { "Fail to procude snapshot. key=${snapshot.globalId.value()}" }
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> = emptyList()
}
