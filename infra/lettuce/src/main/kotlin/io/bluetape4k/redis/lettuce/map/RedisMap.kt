package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.awaitSuspending
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import java.util.concurrent.CompletableFuture

/**
 * Lettuce Redis 클라이언트를 이용한 분산 Map(Distributed Map) 구현체입니다.
 *
 * Redis의 Hash 자료구조(HSET/HGET/HDEL 등)를 사용하여 분산 Map을 구현합니다.
 * Redisson의 RMap을 참고하여 동기, 비동기(CompletableFuture), 코루틴(suspend) 3가지 방식을 모두 지원합니다.
 *
 * ```kotlin
 * val map = RedisMap(connection, "my-map")
 *
 * // 동기 방식
 * map.put("key", "value")
 * val value = map.get("key")
 *
 * // 코루틴 방식
 * map.putSuspending("key", "value")
 * val value = map.getSuspending("key")
 * ```
 *
 * @param connection Lettuce StatefulRedisConnection (StringCodec 기반)
 * @param mapKey Redis에 저장될 Hash 키
 */
class RedisMap(
    private val connection: StatefulRedisConnection<String, String>,
    val mapKey: String,
) {
    companion object: KLogging()

    init {
        require(mapKey.isNotBlank()) { "mapKey는 공백이 아니어야 합니다." }
    }

    private val syncCommands: RedisCommands<String, String> get() = connection.sync()
    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    // =========================================================================
    // 동기 API
    // =========================================================================

    /**
     * 지정한 필드의 값을 반환합니다.
     *
     * @param field 조회할 필드명
     * @return 필드 값 (존재하지 않으면 null)
     */
    fun get(field: String): String? =
        syncCommands.hget(mapKey, field)

    /**
     * 필드에 값을 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 새 필드가 추가됐으면 true, 기존 필드가 업데이트됐으면 false
     */
    fun put(field: String, value: String): Boolean {
        val result = syncCommands.hset(mapKey, field, value)
        log.debug { "RedisMap put: mapKey=$mapKey, field=$field, isNew=$result" }
        return result
    }

    /**
     * 필드가 존재하지 않을 때만 값을 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 설정 성공 여부 (이미 존재하면 false)
     */
    fun putIfAbsent(field: String, value: String): Boolean {
        val result = syncCommands.hsetnx(mapKey, field, value)
        log.debug { "RedisMap putIfAbsent: mapKey=$mapKey, field=$field, result=$result" }
        return result
    }

    /**
     * 지정한 필드를 삭제합니다.
     *
     * @param field 삭제할 필드명
     * @return 삭제된 필드 수
     */
    fun remove(field: String): Long {
        val count = syncCommands.hdel(mapKey, field)
        log.debug { "RedisMap remove: mapKey=$mapKey, field=$field, count=$count" }
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
    fun values(): List<String> =
        syncCommands.hvals(mapKey)

    /**
     * 모든 필드-값 쌍을 반환합니다.
     *
     * @return 필드-값 Map
     */
    fun entries(): Map<String, String> =
        syncCommands.hgetall(mapKey)

    /**
     * 여러 필드-값 쌍을 한번에 설정합니다.
     *
     * @param map 설정할 필드-값 쌍
     */
    fun putAll(map: Map<String, String>) {
        if (map.isEmpty()) return
        syncCommands.hset(mapKey, map)
        log.debug { "RedisMap putAll: mapKey=$mapKey, count=${map.size}" }
    }

    /**
     * 여러 필드의 값을 한번에 조회합니다.
     * 존재하지 않는 필드는 null 값으로 반환됩니다.
     *
     * @param fields 조회할 필드명 컬렉션
     * @return 필드명 → 값 Map (없는 필드는 null)
     */
    fun getAll(fields: Collection<String>): Map<String, String?> {
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
        log.debug { "RedisMap clear: mapKey=$mapKey" }
        return count
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
    fun getAsync(field: String): CompletableFuture<String?> =
        asyncCommands.hget(mapKey, field).toCompletableFuture()

    /**
     * 필드에 값을 비동기로 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 새 필드 추가 여부를 담은 CompletableFuture
     */
    fun putAsync(field: String, value: String): CompletableFuture<Boolean> =
        asyncCommands.hset(mapKey, field, value).toCompletableFuture()
            .thenApply { result ->
                log.debug { "RedisMap putAsync: mapKey=$mapKey, field=$field, isNew=$result" }
                result
            }

    /**
     * 필드가 존재하지 않을 때만 값을 비동기로 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 설정 성공 여부를 담은 CompletableFuture
     */
    fun putIfAbsentAsync(field: String, value: String): CompletableFuture<Boolean> =
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
    fun valuesAsync(): CompletableFuture<List<String>> =
        asyncCommands.hvals(mapKey).toCompletableFuture()

    /**
     * 모든 필드-값 쌍을 비동기로 반환합니다.
     *
     * @return 필드-값 Map을 담은 CompletableFuture
     */
    fun entriesAsync(): CompletableFuture<Map<String, String>> =
        asyncCommands.hgetall(mapKey).toCompletableFuture()

    /**
     * 여러 필드-값 쌍을 비동기로 설정합니다.
     *
     * @param map 설정할 필드-값 쌍
     * @return 완료를 나타내는 CompletableFuture
     */
    fun putAllAsync(map: Map<String, String>): CompletableFuture<Unit> {
        if (map.isEmpty()) return CompletableFuture.completedFuture(Unit)
        return asyncCommands.hset(mapKey, map).toCompletableFuture()
            .thenApply {
                log.debug { "RedisMap putAllAsync: mapKey=$mapKey, count=${map.size}" }
            }
    }

    /**
     * 여러 필드의 값을 비동기로 조회합니다.
     *
     * @param fields 조회할 필드명 컬렉션
     * @return 필드명 → 값 Map을 담은 CompletableFuture (없는 필드는 null)
     */
    fun getAllAsync(fields: Collection<String>): CompletableFuture<Map<String, String?>> {
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
                log.debug { "RedisMap clearAsync: mapKey=$mapKey" }
                count
            }

    // =========================================================================
    // 코루틴 API (suspend)
    // =========================================================================

    /**
     * 지정한 필드의 값을 코루틴으로 반환합니다.
     *
     * @param field 조회할 필드명
     * @return 필드 값 (존재하지 않으면 null)
     */
    suspend fun getSuspending(field: String): String? =
        asyncCommands.hget(mapKey, field).awaitSuspending()

    /**
     * 필드에 값을 코루틴으로 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 새 필드가 추가됐으면 true, 기존 필드가 업데이트됐으면 false
     */
    suspend fun putSuspending(field: String, value: String): Boolean {
        val result = asyncCommands.hset(mapKey, field, value).awaitSuspending()
        log.debug { "RedisMap putSuspending: mapKey=$mapKey, field=$field, isNew=$result" }
        return result
    }

    /**
     * 필드가 존재하지 않을 때만 값을 코루틴으로 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 설정 성공 여부
     */
    suspend fun putIfAbsentSuspending(field: String, value: String): Boolean =
        asyncCommands.hsetnx(mapKey, field, value).awaitSuspending()

    /**
     * 지정한 필드를 코루틴으로 삭제합니다.
     *
     * @param field 삭제할 필드명
     * @return 삭제된 필드 수
     */
    suspend fun removeSuspending(field: String): Long =
        asyncCommands.hdel(mapKey, field).awaitSuspending()

    /**
     * 지정한 필드가 존재하는지 코루틴으로 확인합니다.
     *
     * @param field 확인할 필드명
     * @return 존재하면 true
     */
    suspend fun containsKeySuspending(field: String): Boolean =
        asyncCommands.hexists(mapKey, field).awaitSuspending()

    /**
     * Map의 필드 수를 코루틴으로 반환합니다.
     *
     * @return 필드 수
     */
    suspend fun sizeSuspending(): Long =
        asyncCommands.hlen(mapKey).awaitSuspending()

    /**
     * Map이 비어있는지 코루틴으로 확인합니다.
     *
     * @return 비어있으면 true
     */
    suspend fun isEmptySuspending(): Boolean = sizeSuspending() == 0L

    /**
     * 모든 필드명을 코루틴으로 반환합니다.
     *
     * @return 필드명 목록
     */
    suspend fun keySetSuspending(): List<String> =
        asyncCommands.hkeys(mapKey).awaitSuspending()

    /**
     * 모든 값을 코루틴으로 반환합니다.
     *
     * @return 값 목록
     */
    suspend fun valuesSuspending(): List<String> =
        asyncCommands.hvals(mapKey).awaitSuspending()

    /**
     * 모든 필드-값 쌍을 코루틴으로 반환합니다.
     *
     * @return 필드-값 Map
     */
    suspend fun entriesSuspending(): Map<String, String> =
        asyncCommands.hgetall(mapKey).awaitSuspending()

    /**
     * 여러 필드-값 쌍을 코루틴으로 설정합니다.
     *
     * @param map 설정할 필드-값 쌍
     */
    suspend fun putAllSuspending(map: Map<String, String>) {
        if (map.isEmpty()) return
        asyncCommands.hset(mapKey, map).awaitSuspending()
        log.debug { "RedisMap putAllSuspending: mapKey=$mapKey, count=${map.size}" }
    }

    /**
     * 여러 필드의 값을 코루틴으로 조회합니다.
     *
     * @param fields 조회할 필드명 컬렉션
     * @return 필드명 → 값 Map (없는 필드는 null)
     */
    suspend fun getAllSuspending(fields: Collection<String>): Map<String, String?> {
        if (fields.isEmpty()) return emptyMap()
        val kvList = asyncCommands.hmget(mapKey, *fields.toTypedArray()).awaitSuspending()
        return kvList.associate { kv -> kv.key to (if (kv.hasValue()) kv.value else null) }
    }

    /**
     * Map을 코루틴으로 전체 삭제합니다.
     *
     * @return 삭제된 키 수
     */
    suspend fun clearSuspending(): Long {
        val count = asyncCommands.del(mapKey).awaitSuspending()
        log.debug { "RedisMap clearSuspending: mapKey=$mapKey" }
        return count
    }
}
