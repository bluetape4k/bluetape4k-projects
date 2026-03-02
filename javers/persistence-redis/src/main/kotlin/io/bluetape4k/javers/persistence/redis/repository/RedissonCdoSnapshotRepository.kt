package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.redis.redisson.RedissonCodecs
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.redisson.api.RListMultimap
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.client.codec.LongCodec

/**
 * Redisson 기반 Redis [CdoSnapshot] 저장소.
 *
 * ## 동작/계약
 * - [RListMultimap]으로 GlobalId별 스냅샷 바이트 배열을 관리한다
 * - [loadSnapshots]는 Multimap에서 조회 후 역순 정렬하여 최신순으로 반환한다
 * - CommitId → Sequence 매핑은 [RMap]에 [LongCodec]으로 저장한다
 * - 코덱 기본값은 [JaversCodecs.LZ4Fory] (LZ4 압축 + Fory 직렬화)이다
 *
 * ```kotlin
 * val repo = RedissonCdoSnapshotRepository("user", redissonClient)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * javers.commit("author", entity)
 * val snapshots = javers.findSnapshots(queryByClass<Person>())
 * ```
 *
 * @param name 저장소 이름 (Redis key prefix에 사용)
 * @param redisson [RedissonClient] 인스턴스
 * @param codec [CdoSnapshot]을 encode/decode 할 [JaversCodec] 인스턴스
 */
class RedissonCdoSnapshotRepository(
    val name: String,
    private val redisson: RedissonClient,
    codec: JaversCodec<ByteArray> = JaversCodecs.LZ4Fory,
): AbstractCdoSnapshotRepository<ByteArray>(codec) {

    companion object: KLogging() {
        private const val SEQUENCE = "sequence"
        private const val SNAPSHOT = "snapshot"
    }

    private val sequenceName: String = "javers:$name:$SEQUENCE"
    private val snapshotName: String = "javers:$name:$SNAPSHOT"

    /**
     * GlobalId 별로 Snapshot 컬렉션을 매핑합니다.
     */
    private val snapshots: RListMultimap<String, ByteArray> =
        redisson.getListMultimap(snapshotName, RedissonCodecs.LZ4ForyComposite)

    /**
     * CommitId: Sequence Number 매핑을 저장하는 Map
     */
    private val commitIdSequences: RMap<String, Long> =
        redisson.getMap(sequenceName, LongCodec())

    override fun getKeys(): Set<String> {
        return snapshots.keySet().sorted().toSet()
            .apply {
                log.trace { "load keys. size=$size" }
            }
    }

    override fun contains(globalIdValue: String): Boolean {
        return snapshots.containsKey(globalIdValue)
    }

    override fun getSeq(commitId: CommitId): Long {
        val seq = commitIdSequences.getOrDefault(commitId.value(), 0L)
        log.trace { "get seq. commitId=${commitId.value()}, seq=$seq" }
        return seq
    }

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        commitIdSequences.fastPut(commitId.value(), sequence)
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        return snapshots[globalIdValue].size
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val key = snapshot.globalId.value()
        val value = encode(snapshot)
        val saved = snapshots.put(key, value)
        log.trace { "Save snapshot [$saved]. key=[$key], version=[${snapshot.version}]" }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        // NOTE: 최신 데이터가 처음에 오도록 역순 정렬해야 합니다. (Stack처럼 사용합니다)
        val loaded = snapshots.getAll(globalIdValue)
            .mapNotNull { value ->
                log.debug { "value size=${value.size}" }
                if (value.isNotEmpty()) decode(value) else null
            }
            .reversed()
        log.trace { "Load snapshots. globalId=$globalIdValue, size=${loaded.size}" }
        return loaded
    }
}
