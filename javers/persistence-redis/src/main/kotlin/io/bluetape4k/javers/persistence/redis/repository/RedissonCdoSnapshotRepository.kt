package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.javers.codecs.GsonCodec
import io.bluetape4k.javers.codecs.GsonCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.redisson.api.RListMultimap
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.client.codec.ByteArrayCodec
import org.redisson.client.codec.LongCodec
import org.redisson.client.codec.StringCodec
import org.redisson.codec.CompositeCodec

/**
 * JaVers [CdoSnapshot] 을 Redisson Library를 이용하여
 * Redis Server에 저장, 로드하는 기능을 제공하는 [AbstractCdoSnapshotRepository] 구현체입니다.
 *
 * @param name repository name
 * @param redisson [RedissonClient] 인스턴스
 * @param codec [CdoSnapshot]을 encode/decode 할 [GsonCodec] 인스턴스
 */
class RedissonCdoSnapshotRepository(
    val name: String,
    private val redisson: RedissonClient,
    codec: GsonCodec<ByteArray> = GsonCodecs.LZ4Kryo,
): AbstractCdoSnapshotRepository<ByteArray>(codec) {

    companion object: KLogging() {
        private const val SEQUENCE_SUFFIX = "sequence"
        private const val SNAPSHOT_SUFFIX = "snapshots"
    }

    private val sequenceName: String = "javers:$SEQUENCE_SUFFIX:$name"
    private val snapshotName: String = "javers:$SNAPSHOT_SUFFIX:$name"

    /**
     * GlobalId 별로 Snapshot 컬렉션을 매핑합니다.
     */
    private val snapshots: RListMultimap<String, ByteArray> =
        redisson.getListMultimap(snapshotName, CompositeCodec(StringCodec(), ByteArrayCodec(), ByteArrayCodec()))

    /**
     * CommitId: Sequence Number 매핑을 저장하는 Map
     */
    private val commitIdSequences: RMap<String, Long> =
        redisson.getMap(sequenceName, LongCodec())

    override fun getKeys(): List<String> {
        return snapshots.keySet().sorted().apply {
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
        // NOTE: 최신 데이터가 처음에 오도록 역순 정렬이 필요합니다. (Stack처럼 사용합니다)
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
