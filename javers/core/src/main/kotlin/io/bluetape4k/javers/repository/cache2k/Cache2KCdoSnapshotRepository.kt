package io.bluetape4k.javers.repository.cache2k

import io.bluetape4k.cache.cache2k.cache2k
import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import org.cache2k.Cache
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Cache2k 기반의 인메모리 [CdoSnapshot] 저장소.
 *
 * ## 동작/계약
 * - [Cache] 인스턴스를 사용하여 GlobalId별 스냅샷 목록을 메모리에 보관한다
 * - 스냅샷은 최신순으로 리스트 앞쪽에 삽입된다 (index 0)
 * - lock으로 동시 쓰기 안전성을 보장한다
 *
 * ```kotlin
 * val repo = Cache2KCdoSnapshotRepository()
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * ```
 *
 * @param codec 스냅샷 인코딩/디코딩에 사용할 [JaversCodec] (기본값: LZ4 압축 문자열)
 */
class Cache2KCdoSnapshotRepository(
    codec: JaversCodec<String> = JaversCodecs.LZ4String,
): AbstractCdoSnapshotRepository<String>(codec) {

    companion object: KLogging()

    private val lock = ReentrantLock()

    private val snapshotCache: Cache<String, MutableList<String>> by lazy {
        cache2k<String, MutableList<String>> {
            this.entryCapacity(100_000)
            this.storeByReference(true)
            this.eternal(true)
        }.build()
    }

    private val commitSeqCache: Cache<CommitId, Long> by lazy {
        cache2k<CommitId, Long> {
            this.entryCapacity(100_000)
            this.eternal(true)
        }.build()
    }

    override fun getKeys(): Set<String> {
        return snapshotCache.keys()
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
            val snapshots = snapshotCache.computeIfAbsent(globalIdValue) { mutableListOf() }
            val encoded = encode(snapshot)
            snapshots.add(0, encoded)
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        return snapshotCache[globalIdValue]?.mapNotNull { decode(it) } ?: emptyList()
    }
}
