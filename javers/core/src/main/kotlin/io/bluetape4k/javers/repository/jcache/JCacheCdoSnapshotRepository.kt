package io.bluetape4k.javers.repository.jcache

import io.bluetape4k.cache.jcache.getOrCreate
import io.bluetape4k.javers.codecs.GsonCodec
import io.bluetape4k.javers.codecs.GsonCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [CdoSnapshot] 저장소로 [javax.cache.Cache] 를 사용하는 Repository 입니다.
 *
 * @param prefix  cache name prefix
 * @param cacheManager [javax.cache.CacheManager] 인스턴스
 * @param codec [CdoSnapshot] 변환을 위한 [GsonCodec] 인스턴스
 */
class JCacheCdoSnapshotRepository(
    prefix: String,
    cacheManager: javax.cache.CacheManager,
    codec: GsonCodec<String> = GsonCodecs.LZ4String,
): AbstractCdoSnapshotRepository<String>(codec) {

    companion object: KLogging() {
        private const val SNAPSHOT_SUFFIX = "-snapshots"
        private const val COMMIT_SEQ_SUFFIX = "-commit_seq"
    }

    private val lock = ReentrantLock()

    private val snapshotCacheName = prefix + SNAPSHOT_SUFFIX
    private val commitSeqCacheName = prefix + COMMIT_SEQ_SUFFIX

    private val snapshotCache: javax.cache.Cache<String, MutableList<String>> =
        cacheManager.getOrCreate(snapshotCacheName)
    private val commitSeqCache: javax.cache.Cache<CommitId, Long> = cacheManager.getOrCreate(commitSeqCacheName)

    override fun getKeys(): List<String> {
        return snapshotCache.map { it.key }
    }

    override fun contains(globalIdValue: String): Boolean {
        return snapshotCache.containsKey(globalIdValue)
    }

    override fun getSeq(commitId: CommitId): Long = commitSeqCache[commitId] ?: 0L

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        commitSeqCache.put(commitId, sequence)
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        return snapshotCache[globalIdValue]?.size ?: 0
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        lock.withLock {
            val globalIdValue = snapshot.globalId.value()

            // NOTE: JCache 는 Reference 가 아닌 Value 를 저장해야 하므로, 매번 Replace 형식이 되어야 한다
            val snapshots = snapshotCache.get(globalIdValue) ?: mutableListOf()
            val encoded = encode(snapshot)
            snapshots.add(0, encoded)
            snapshotCache.put(globalIdValue, snapshots)
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        return snapshotCache[globalIdValue]?.mapNotNull { decode(it) } ?: emptyList()
    }
}
