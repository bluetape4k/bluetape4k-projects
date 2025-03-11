package io.bluetape4k.javers.repository.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.javers.codecs.GsonCodec
import io.bluetape4k.javers.codecs.GsonCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [CdoSnapshot] 저장소로 [com.github.benmanes.caffeine.cache.Cache] 를 사용하는 Repository 입니다.
 *
 * @param codec [CdoSnapshot] 변환을 위한 [GsonCodec] 인스턴스
 */
class CaffeineCdoSnapshotRepository(
    codec: GsonCodec<String> = GsonCodecs.LZ4String,
): AbstractCdoSnapshotRepository<String>(codec) {

    companion object: KLogging()

    private val lock = ReentrantLock()

    /**
     * [CdoSnapshot] 컬렉션을 저장하는 Cache (key=globalId, value=collection of encoded snapshot)
     */
    private val snapshotCache: Cache<String, MutableList<String>> by lazy {
        caffeine {
            initialCapacity(1_000)
        }.build()
    }

    /**
     * [CommitId] - Sequence Number를 캐시합니다.
     */
    private val commitSeqCache: Cache<CommitId, Long> by lazy {
        caffeine {
            initialCapacity(1_000)
        }.build()
    }

    override fun getKeys(): List<String> {
        return snapshotCache.asMap().map { it.key }
    }

    override fun contains(globalIdValue: String): Boolean {
        return snapshotCache.getIfPresent(globalIdValue) != null
    }

    override fun getSeq(commitId: CommitId): Long = commitSeqCache.getIfPresent(commitId) ?: 0L

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        commitSeqCache.put(commitId, sequence)
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        return snapshotCache.getIfPresent(globalIdValue)?.size ?: 0
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        lock.withLock {
            val globalIdValue = snapshot.globalId.value()
            val snapshots = snapshotCache.get(globalIdValue) { _ -> mutableListOf() }
            val encoded = encode(snapshot)
            snapshots.add(0, encoded)
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        return snapshotCache.getIfPresent(globalIdValue)?.mapNotNull { decode(it) } ?: emptyList()
    }
}
