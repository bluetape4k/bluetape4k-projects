package io.bluetape4k.javers.persistence.redis.repository

import io.bluetape4k.javers.codecs.JaversCodec
import io.bluetape4k.javers.codecs.JaversCodecs
import io.bluetape4k.javers.repository.AbstractCdoSnapshotRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.support.asByteArray
import io.bluetape4k.support.asInt
import io.bluetape4k.support.asLongOrNull
import io.lettuce.core.RedisClient
import org.javers.core.commit.CommitId
import org.javers.core.metamodel.`object`.CdoSnapshot

/**
 * Lettuce 기반 Redis [CdoSnapshot] 저장소.
 *
 * ## 동작/계약
 * - GlobalId별 스냅샷을 Redis LIST(`javers:{name}:snapshot:{globalId}`)에 LPUSH로 최신순 저장한다
 * - [saveSnapshot]은 MULTI/EXEC 트랜잭션으로 LIST 저장 + HSET GlobalId 등록을 원자적으로 수행한다
 * - 트랜잭션 실패 시 DISCARD 후 예외를 로깅한다
 * - 코덱 기본값은 [JaversCodecs.LZ4Fory] (LZ4 압축 + Fory 직렬화)이다
 *
 * ```kotlin
 * val repo = LettuceCdoSnapshotRepository("user", redisClient)
 * val javers = JaversBuilder.javers()
 *     .registerJaversRepository(repo)
 *     .build()
 * javers.commit("author", entity)
 * val snapshots = javers.findSnapshots(queryByClass<Person>())
 * ```
 *
 * @param name 저장소 이름 (Redis key prefix에 사용)
 * @param client Lettuce [RedisClient] 인스턴스
 * @param codec [CdoSnapshot]을 encode/decode 할 [JaversCodec] 인스턴스
 */
class LettuceCdoSnapshotRepository(
    val name: String,
    private val client: RedisClient,
    codec: JaversCodec<ByteArray> = JaversCodecs.LZ4Fory,
): AbstractCdoSnapshotRepository<ByteArray>(codec) {

    companion object: KLogging() {
        private const val CACHE_KEY_SET = "globalId:set"
        private const val SEQUENCE_SET = "sequence:set"
        private const val SNAPSHOT_SUFFIX = "snapshot:"
    }

    // Cache Key에 해당하는 [GlobalId.value()] 값을 저장하는 Set
    private val cacheSetKey: String = "javers:$name:$CACHE_KEY_SET"

    // HSET CommitId SEQUENCE NO 를 보관하는 Set
    private val sequenceSetKey: String = "javers:$name:$SEQUENCE_SET"

    // [CdoSnapshot]을 저장할 Redis LIST Key 값의 prefix 입니다.
    // globalId 마다 List<CdoSnapshot> 을 저장합니다.
    private val snapshotPrefix = "javers:$name:$SNAPSHOT_SUFFIX"

    private val commands by lazy {
        LettuceClients.commands(client, codec = LettuceBinaryCodecs.lz4Fory())
    }

    override fun getKeys(): Set<String> {
        return commands.hkeys(cacheSetKey).sorted().toSet()
            .apply {
                log.trace { "load keys. size=${size}" }
            }
    }

    override fun contains(globalIdValue: String): Boolean {
        return commands.hexists(cacheSetKey, globalIdValue) ?: false
    }

    override fun getSeq(commitId: CommitId): Long {
        val seq = commands.hget(sequenceSetKey, commitId.value())?.asLongOrNull() ?: 0L
        log.trace { "get seq. commitId=${commitId.value()}, seq=$seq" }
        return seq
    }

    override fun updateCommitId(commitId: CommitId, sequence: Long) {
        commands.hset(sequenceSetKey, commitId.value(), sequence.toString())
    }

    override fun getSnapshotSize(globalIdValue: String): Int {
        val snapshotSize = commands.llen(makeSnapshotKey(globalIdValue)).asInt()
        log.trace { "Get snapshot size=${snapshotSize}, globalId=$globalIdValue" }
        return snapshotSize
    }

    override fun saveSnapshot(snapshot: CdoSnapshot) {
        val key = makeSnapshotKey(snapshot.globalId.value())
        val value = encode(snapshot)

        try {
            commands.multi()
            // 최신 Snapshot 을 저장합니다.
            commands.lpush(key, value)
            // 전체 Cache Item의 GlobalId 를 빠르게 조회하기 위해 따로 저장한다
            commands.hset(cacheSetKey, snapshot.globalId.value(), snapshot.version)
            commands.exec()
            log.debug { "Save snapshot key=$key, version=${snapshot.version}" }
        } catch (e: Exception) {
            log.error(e) { "Fail to save snapshot. snapshot globalId=${snapshot.globalId.value()}" }
            commands.discard()
        }
    }

    override fun loadSnapshots(globalIdValue: String): List<CdoSnapshot> {
        val snapshots = commands
            .lrange(makeSnapshotKey(globalIdValue), 0, -1)
            .mapNotNull { decode(it.asByteArray()) }
        log.trace { "Load snapshots. globalId=$globalIdValue, size=${snapshots.size}" }
        return snapshots
    }

    /**
     * Make snapshot key (eg. `javers:user:snapshots:id`)
     *
     * @param id [CdoSnapshot]의 GlobalId 값 (eg. `User/1` )
     * @return Snapshot 을 기록하는 Redis List 의 Key 값 (eg. `javers:user:snapshots:User/1`)
     */
    private fun makeSnapshotKey(id: String): String {
        return snapshotPrefix + id
    }
}
