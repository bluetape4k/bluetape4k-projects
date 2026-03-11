package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.HSetExArgs
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * Lettuce Redis 클라이언트를 이용한 분산 Map(Distributed Map) 구현체입니다.
 *
 * Redis의 Hash 자료구조(HSET/HGET/HDEL 등)를 사용하여 분산 Map을 구현합니다.
 * 동기(sync)와 비동기(CompletableFuture) 두 가지 방식을 지원합니다.
 * 코루틴(suspend) 방식은 [LettuceSuspendMap]을 사용하세요.
 *
 * ```kotlin
 * val codec = LettuceBinaryCodecs.lz4Fory<MyData>()
 * val connection = redisClient.connect(codec)
 * val map = LettuceMap<MyData>(connection, "my-map")
 *
 * // 동기 방식
 * map.put("key", myData)
 * val value = map.get("key")
 *
 * // 비동기 방식
 * map.putAsync("key", myData)
 * val future = map.getAsync("key")
 * ```
 *
 * @param V 값 타입
 * @param connection Lettuce StatefulRedisConnection (LettuceBinaryCodec<V> 기반)
 * @param mapKey Redis에 저장될 Hash 키
 */
open class LettuceMap<V: Any>(
    private val connection: StatefulRedisConnection<String, V>,
    val mapKey: String,
) {
    companion object: KLogging()

    init {
        mapKey.requireNotBlank("mapKey")
    }

    protected val syncCommands: RedisCommands<String, V> get() = connection.sync()
    private val asyncCommands: RedisAsyncCommands<String, V> get() = connection.async()

    // =========================================================================
    // 동기 API
    // =========================================================================

    /**
     * 지정한 필드의 값을 반환합니다.
     *
     * @param field 조회할 필드명
     * @return 필드 값 (존재하지 않으면 null)
     */
    fun get(field: String): V? =
        syncCommands.hget(mapKey, field)

    /**
     * 필드에 값을 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 새 필드가 추가됐으면 true, 기존 필드가 업데이트됐으면 false
     */
    fun put(field: String, value: V): Boolean {
        val result = syncCommands.hset(mapKey, field, value)
        log.debug { "LettuceMap put: mapKey=$mapKey, field=$field, isNew=$result" }
        return result
    }

    /**
     * 필드가 존재하지 않을 때만 값을 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 설정 성공 여부 (이미 존재하면 false)
     */
    fun putIfAbsent(field: String, value: V): Boolean {
        val result = syncCommands.hsetnx(mapKey, field, value)
        log.debug { "LettuceMap putIfAbsent: mapKey=$mapKey, field=$field, result=$result" }
        return result
    }

    /**
     * 필드가 존재하지 않을 때만 값을 설정하고, 성공한 경우 Hash key TTL을 함께 갱신합니다.
     *
     * Redis Hash field 단위의 조건부 TTL 설정 명령이 없어 `HSETNX` 성공 후 `EXPIRE`를 적용합니다.
     * 이 모듈의 TTL 계약은 field가 아닌 Hash key([mapKey]) 전체의 만료를 갱신하는 방식입니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @param ttl 적용할 Hash key TTL, null이면 일반 [putIfAbsent]와 동일하게 동작
     * @return 설정 성공 여부 (이미 존재하면 false)
     */
    fun putIfAbsentTtl(field: String, value: V, ttl: Duration?): Boolean {
        val added = putIfAbsent(field, value)
        if (added && ttl != null) {
            syncCommands.expire(mapKey, ttl)
        }
        return added
    }

    /**
     * 지정한 필드를 삭제합니다.
     *
     * @param field 삭제할 필드명
     * @return 삭제된 필드 수
     */
    fun remove(field: String): Long {
        val count = syncCommands.hdel(mapKey, field)
        log.debug { "LettuceMap remove: mapKey=$mapKey, field=$field, count=$count" }
        return count
    }

    /**
     * 지정한 필드가 존재하는지 확인합니다.
     *
     * @param field 확인할 필드명
     * @return 존재하면 true
     */
    fun containsKey(field: String): Boolean =
        syncCommands.hexists(mapKey, field)

    /**
     * Map의 필드 수(크기)를 반환합니다.
     *
     * @return 필드 수
     */
    fun size(): Long =
        syncCommands.hlen(mapKey)

    /**
     * Map이 비어있는지 확인합니다.
     *
     * @return 비어있으면 true
     */
    fun isEmpty(): Boolean = size() == 0L

    /**
     * 모든 필드명을 반환합니다.
     *
     * @return 필드명 목록
     */
    fun keySet(): List<String> =
        syncCommands.hkeys(mapKey)

    /**
     * 모든 값을 반환합니다.
     *
     * @return 값 목록
     */
    fun values(): List<V> =
        syncCommands.hvals(mapKey)

    /**
     * 모든 필드-값 쌍을 반환합니다.
     *
     * @return 필드-값 Map
     */
    fun entries(): Map<String, V> =
        syncCommands.hgetall(mapKey)

    /**
     * 여러 필드-값 쌍을 한번에 설정합니다.
     *
     * @param map 설정할 필드-값 쌍
     */
    fun putAll(map: Map<String, V>) {
        if (map.isEmpty()) return
        syncCommands.hset(mapKey, map)
        log.debug { "LettuceMap putAll: mapKey=$mapKey, count=${map.size}" }
    }

    /**
     * 여러 필드의 값을 한번에 조회합니다.
     * 존재하지 않는 필드는 null 값으로 반환됩니다.
     *
     * @param fields 조회할 필드명 컬렉션
     * @return 필드명 → 값 Map (없는 필드는 null)
     */
    fun getAll(fields: Collection<String>): Map<String, V?> {
        if (fields.isEmpty()) return emptyMap()
        val kvList = syncCommands.hmget(mapKey, *fields.toTypedArray())
        return kvList.associate { kv -> kv.key to (if (kv.hasValue()) kv.value else null) }
    }

    /**
     * Map 전체를 삭제합니다. (Redis Hash 키 삭제)
     *
     * @return 삭제된 키 수 (키가 존재했으면 1, 없었으면 0)
     */
    fun clear(): Long {
        val count = syncCommands.del(mapKey)
        log.debug { "LettuceMap clear: mapKey=$mapKey" }
        return count
    }

    /**
     * 필드에 값을 TTL과 함께 설정합니다. TTL이 null이면 일반 put과 동일합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @param ttl Hash key TTL 설정 (null이면 TTL 없음)
     * @return 저장 성공 여부
     */
    fun putTtl(field: String, value: V, ttl: Duration?): Boolean {
        if (ttl != null) {
            val ttlArgs = HSetExArgs.Builder.ex(ttl)
            val hsetex = runCatching {
                syncCommands.hsetex(mapKey, ttlArgs, mapOf(field to value))
                syncCommands.expire(mapKey, ttl)
                true
            }.recoverCatching { error ->
                if (error is RedisCommandExecutionException && error.message?.contains("unknown command", ignoreCase = true) == true) {
                    val added = put(field, value)
                    syncCommands.expire(mapKey, ttl)
                    added
                } else {
                    throw error
                }
            }.getOrThrow()
            log.debug { "LettuceMap putTtl: mapKey=$mapKey, field=$field, ttl=$ttl" }
            return hsetex
        }
        val added = put(field, value)
        log.debug { "LettuceMap putTtl: mapKey=$mapKey, field=$field, ttl=$ttl" }
        return added
    }

    /**
     * 여러 필드-값 쌍을 TTL과 함께 설정합니다.
     *
     * @param entries 설정할 필드-값 쌍
     * @param ttl Hash key TTL 설정 (null이면 TTL 없음)
     */
    fun putAllTtl(entries: Map<String, V>, ttl: Duration?) {
        if (entries.isEmpty()) return
        if (ttl == null) {
            putAll(entries)
            return
        }
        val ttlArgs = HSetExArgs.Builder.ex(ttl)
        runCatching {
            syncCommands.hsetex(mapKey, ttlArgs, entries)
            syncCommands.expire(mapKey, ttl)
        }.recoverCatching { error ->
            if (error is RedisCommandExecutionException && error.message?.contains("unknown command", ignoreCase = true) == true) {
                syncCommands.hset(mapKey, entries)
                syncCommands.expire(mapKey, ttl)
            } else {
                throw error
            }
        }.getOrThrow()
        log.debug { "LettuceMap putAllTtl: mapKey=$mapKey, count=${entries.size}, ttl=$ttl" }
    }

    // =========================================================================
    // 비동기 API (CompletableFuture)
    // =========================================================================

    /**
     * 지정한 필드의 값을 비동기로 반환합니다.
     *
     * @param field 조회할 필드명
     * @return 필드 값을 담은 CompletableFuture (없으면 null)
     */
    fun getAsync(field: String): CompletableFuture<V?> =
        asyncCommands.hget(mapKey, field).toCompletableFuture()

    /**
     * 필드에 값을 비동기로 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 새 필드 추가 여부를 담은 CompletableFuture
     */
    fun putAsync(field: String, value: V): CompletableFuture<Boolean> =
        asyncCommands.hset(mapKey, field, value).toCompletableFuture()
            .thenApply { result ->
                log.debug { "LettuceMap putAsync: mapKey=$mapKey, field=$field, isNew=$result" }
                result
            }

    /**
     * 필드가 존재하지 않을 때만 값을 비동기로 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 설정 성공 여부를 담은 CompletableFuture
     */
    fun putIfAbsentAsync(field: String, value: V): CompletableFuture<Boolean> =
        asyncCommands.hsetnx(mapKey, field, value).toCompletableFuture()

    /**
     * 지정한 필드를 비동기로 삭제합니다.
     *
     * @param field 삭제할 필드명
     * @return 삭제된 필드 수를 담은 CompletableFuture
     */
    fun removeAsync(field: String): CompletableFuture<Long> =
        asyncCommands.hdel(mapKey, field).toCompletableFuture()

    /**
     * 지정한 필드가 존재하는지 비동기로 확인합니다.
     *
     * @param field 확인할 필드명
     * @return 존재 여부를 담은 CompletableFuture
     */
    fun containsKeyAsync(field: String): CompletableFuture<Boolean> =
        asyncCommands.hexists(mapKey, field).toCompletableFuture()

    /**
     * Map의 필드 수를 비동기로 반환합니다.
     *
     * @return 필드 수를 담은 CompletableFuture
     */
    fun sizeAsync(): CompletableFuture<Long> =
        asyncCommands.hlen(mapKey).toCompletableFuture()

    /**
     * Map이 비어있는지 비동기로 확인합니다.
     *
     * @return 비어있으면 true를 담은 CompletableFuture
     */
    fun isEmptyAsync(): CompletableFuture<Boolean> =
        sizeAsync().thenApply { it == 0L }

    /**
     * 모든 필드명을 비동기로 반환합니다.
     *
     * @return 필드명 목록을 담은 CompletableFuture
     */
    fun keySetAsync(): CompletableFuture<List<String>> =
        asyncCommands.hkeys(mapKey).toCompletableFuture()

    /**
     * 모든 값을 비동기로 반환합니다.
     *
     * @return 값 목록을 담은 CompletableFuture
     */
    fun valuesAsync(): CompletableFuture<List<V>> =
        asyncCommands.hvals(mapKey).toCompletableFuture()

    /**
     * 모든 필드-값 쌍을 비동기로 반환합니다.
     *
     * @return 필드-값 Map을 담은 CompletableFuture
     */
    fun entriesAsync(): CompletableFuture<Map<String, V>> =
        asyncCommands.hgetall(mapKey).toCompletableFuture()

    /**
     * 여러 필드-값 쌍을 비동기로 설정합니다.
     *
     * @param map 설정할 필드-값 쌍
     * @return 완료를 나타내는 CompletableFuture
     */
    fun putAllAsync(map: Map<String, V>): CompletableFuture<Unit> {
        if (map.isEmpty()) return CompletableFuture.completedFuture(Unit)
        return asyncCommands.hset(mapKey, map).toCompletableFuture()
            .thenApply {
                log.debug { "LettuceMap putAllAsync: mapKey=$mapKey, count=${map.size}" }
            }
    }

    /**
     * 여러 필드의 값을 비동기로 조회합니다.
     *
     * @param fields 조회할 필드명 컬렉션
     * @return 필드명 → 값 Map을 담은 CompletableFuture (없는 필드는 null)
     */
    fun getAllAsync(fields: Collection<String>): CompletableFuture<Map<String, V?>> {
        if (fields.isEmpty()) return CompletableFuture.completedFuture(emptyMap())
        return asyncCommands.hmget(mapKey, *fields.toTypedArray()).toCompletableFuture()
            .thenApply { kvList ->
                kvList.associate { kv -> kv.key to (if (kv.hasValue()) kv.value else null) }
            }
    }

    /**
     * Map을 비동기로 전체 삭제합니다.
     *
     * @return 삭제된 키 수를 담은 CompletableFuture
     */
    fun clearAsync(): CompletableFuture<Long> =
        asyncCommands.del(mapKey).toCompletableFuture()
            .thenApply { count ->
                log.debug { "LettuceMap clearAsync: mapKey=$mapKey" }
                count
            }
}
