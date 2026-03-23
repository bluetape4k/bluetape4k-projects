package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await

/**
 * Lettuce Redis 클라이언트를 이용한 분산 Map의 코루틴 구현체입니다.
 *
 * [LettuceMap]의 코루틴(suspend) 버전으로, Redis Hash 자료구조(HSET/HGET/HDEL 등)를
 * suspend 함수를 통해 사용합니다.
 * 동기/비동기(CompletableFuture) 방식은 [LettuceMap]을 사용하세요.
 *
 * ```kotlin
 * val codec = LettuceBinaryCodecs.lz4Fory<MyData>()
 * val connection = redisClient.connect(codec)
 * val suspendMap = LettuceSuspendMap<MyData>(connection, "my-map")
 *
 * val value = suspendMap.get("key")
 * suspendMap.put("key", myData)
 * ```
 *
 * @param V 값 타입
 * @param connection Lettuce StatefulRedisConnection (LettuceBinaryCodec<V> 기반)
 * @param mapKey Redis에 저장될 Hash 키
 */
open class LettuceSuspendMap<V: Any>(
    private val connection: StatefulRedisConnection<String, V>,
    val mapKey: String,
) {
    companion object: KLogging()

    init {
        mapKey.requireNotBlank("mapKey")
    }

    private val asyncCommands: RedisAsyncCommands<String, V> get() = connection.async()

    /**
     * 지정한 필드의 값을 코루틴으로 반환합니다.
     *
     * @param field 조회할 필드명
     * @return 필드 값 (존재하지 않으면 null)
     */
    suspend fun get(field: String): V? = asyncCommands.hget(mapKey, field).await()

    /**
     * 필드에 값을 코루틴으로 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 새 필드가 추가됐으면 true, 기존 필드가 업데이트됐으면 false
     */
    suspend fun put(
        field: String,
        value: V,
    ): Boolean {
        val result = asyncCommands.hset(mapKey, field, value).await()
        log.debug { "LettuceSuspendMap put: mapKey=$mapKey, field=$field, isNew=$result" }
        return result
    }

    /**
     * 필드가 존재하지 않을 때만 값을 코루틴으로 설정합니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @return 설정 성공 여부 (이미 존재하면 false)
     */
    suspend fun putIfAbsent(
        field: String,
        value: V,
    ): Boolean = asyncCommands.hsetnx(mapKey, field, value).await()

    /**
     * 지정한 필드를 코루틴으로 삭제합니다.
     *
     * @param field 삭제할 필드명
     * @return 삭제된 필드 수
     */
    suspend fun remove(field: String): Long = asyncCommands.hdel(mapKey, field).await()

    /**
     * 지정한 필드가 존재하는지 코루틴으로 확인합니다.
     *
     * @param field 확인할 필드명
     * @return 존재하면 true
     */
    suspend fun containsKey(field: String): Boolean = asyncCommands.hexists(mapKey, field).await()

    /**
     * Map의 필드 수를 코루틴으로 반환합니다.
     *
     * @return 필드 수
     */
    suspend fun size(): Long = asyncCommands.hlen(mapKey).await()

    /**
     * Map이 비어있는지 코루틴으로 확인합니다.
     *
     * @return 비어있으면 true
     */
    suspend fun isEmpty(): Boolean = size() == 0L

    /**
     * 모든 필드명을 코루틴으로 반환합니다.
     *
     * @return 필드명 목록
     */
    suspend fun keySet(): List<String> = asyncCommands.hkeys(mapKey).await()

    /**
     * 모든 값을 코루틴으로 반환합니다.
     *
     * @return 값 목록
     */
    suspend fun values(): List<V> = asyncCommands.hvals(mapKey).await()

    /**
     * 모든 필드-값 쌍을 코루틴으로 반환합니다.
     *
     * @return 필드-값 Map
     */
    suspend fun entries(): Map<String, V> = asyncCommands.hgetall(mapKey).await()

    /**
     * 여러 필드-값 쌍을 코루틴으로 설정합니다.
     *
     * @param map 설정할 필드-값 쌍
     */
    suspend fun putAll(map: Map<String, V>) {
        if (map.isEmpty()) return
        asyncCommands.hset(mapKey, map).await()
        log.debug { "LettuceSuspendMap putAll: mapKey=$mapKey, count=${map.size}" }
    }

    /**
     * 여러 필드의 값을 코루틴으로 조회합니다.
     * 존재하지 않는 필드는 null 값으로 반환됩니다.
     *
     * @param fields 조회할 필드명 컬렉션
     * @return 필드명 → 값 Map (없는 필드는 null)
     */
    suspend fun getAll(fields: Collection<String>): Map<String, V?> {
        if (fields.isEmpty()) return emptyMap()
        val kvList = asyncCommands.hmget(mapKey, *fields.toTypedArray()).await()
        return kvList.associate { kv -> kv.key to (if (kv.hasValue()) kv.value else null) }
    }

    /**
     * Map을 코루틴으로 전체 삭제합니다. (Redis Hash 키 삭제)
     *
     * @return 삭제된 키 수 (키가 존재했으면 1, 없었으면 0)
     */
    suspend fun clear(): Long {
        val count = asyncCommands.del(mapKey).await()
        log.debug { "LettuceSuspendMap clear: mapKey=$mapKey" }
        return count
    }
}
