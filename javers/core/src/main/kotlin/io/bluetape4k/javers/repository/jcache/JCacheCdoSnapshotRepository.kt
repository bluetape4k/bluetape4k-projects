package io.bluetape4k.javers.repository.jcache

import io.bluetape4k.cache.jcache.getOrCreate
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.util.concurrent.locks.ReentrantLock
import javax.cache.expiry.EternalExpiryPolicy
import kotlin.concurrent.withLock

/**
 * JCache(JSR-107) 기반의 [CdoSnapshot] 저장소.
 *
 * ## 동작/계약
 * - [javax.cache.Cache]를 사용하여 GlobalId별 스냅샷 목록을 보관한다
 * - JCache는 기본적으로 값 복사(store by value)로 동작하므로, 쓰기 시 put을 다시 호출해야 한다
 * - lock으로 동시 쓰기 안전성을 보장한다
 *
 * @param prefix 캐시 이름 접두사
 * @param cacheManager [javax.cache.CacheManager] 인스턴스
 * @param codec 스냅샷 인코딩/디코딩에 사용할 [JaversCodec] (기본값: LZ4 압축 문자열)
 */
class JCacheCdoSnapshotRepository(
    prefix: String,
    cacheManager: javax.cache.CacheManager,
    codec: JaversCodec<String> = JaversCodecs.LZ4String,
): AbstractCdoSnapshotRepository<String>(codec) {

    companion object: KLogging() {
        private const val SNAPSHOT_SUFFIX = "-snapshots"
        private const val COMMIT_SEQ_SUFFIX = "-commit_seq"
    }

    private val lock = ReentrantLock()

    private val snapshotCacheName = prefix + SNAPSHOT_SUFFIX
    private val commitSeqCacheName = prefix + COMMIT_SEQ_SUFFIX

    private val snapshotCache: javax.cache.Cache<String, MutableList<String>> by lazy {
        val cfg = jcacheConfiguration<String, MutableList<String>> {
            setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
            setStoreByValue(true)
        }
        cacheManager.getOrCreate(snapshotCacheName, cfg)
    }
    private val commitSeqCache: javax.cache.Cache<CommitId, Long> by lazy {
        val cfg = jcacheConfiguration<CommitId, Long> {
            setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
            setStoreByValue(true)
        }
        cacheManager.getOrCreate(commitSeqCacheName, cfg)
    }

    override fun getKeys(): Set<String> {
        return snapshotCache.map { it.key }.toSet()
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

            // NOTE: JCache 는 기본적으로 Reference를 저장하지만, 여기서는 Value로 저장해야 합니다.
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
