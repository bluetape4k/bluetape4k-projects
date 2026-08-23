package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.HSetExArgs
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.LockSupport

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
    val supportsHSetEx: Boolean = false,
) {
    companion object: KLogging() {
        private const val LOCK_SUFFIX = ":__bluetape4k:lock"
        private const val LOCK_RETRY_DELAY_MILLIS = 50L
        private const val LOCK_RETRY_DELAY_NANOS = LOCK_RETRY_DELAY_MILLIS * 1_000_000L
        private const val RELEASE_LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
    }

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
     * ```kotlin
     * val map = LettuceMap<Int>(connection, "my-map")
     * map.put("hello", 5)
     * val value = map.get("hello")
     * // value == 5
     * val missing = map.get("notexist")
     * // missing == null
     * ```
     *
     * @param field 조회할 필드명
     * @return 필드 값 (존재하지 않으면 null)
     */
    fun get(field: String): V? =
        syncCommands.hget(mapKey, field)

    /**
     * 필드에 값을 설정합니다.
     *
     * ```kotlin
     * val map = LettuceMap<Int>(connection, "my-map")
     * val isNew = map.put("hello", 5)
     * // isNew == true  (새 필드)
     * val isNew2 = map.put("hello", 10)
     * // isNew2 == false (기존 필드 갱신)
     * ```
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
     * ```kotlin
     * val map = LettuceMap<Int>(connection, "my-map")
     * map.put("hello", 5)
     * val count = map.remove("hello")
     * // count == 1
     * val count2 = map.remove("notexist")
     * // count2 == 0
     * ```
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
     * ```kotlin
     * val map = LettuceMap<Int>(connection, "my-map")
     * map.putAll(mapOf("a" to 1, "b" to 2))
     * val result = map.getAll(listOf("a", "b", "c"))
     * // result == {"a" to 1, "b" to 2, "c" to null}
     * ```
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
     * Hash 전체에 TTL을 적용합니다.
     *
     * [putTtl]이 Redis 8의 HSETEX 경로를 선택하면 field TTL만 갱신하므로,
     * hash 키 수명 계약을 사용하는 상위 캐시가 필요할 때 이 메서드를 함께 호출합니다.
     */
    fun refreshTtl(ttl: Duration?) {
        if (ttl != null) syncCommands.expire(mapKey, ttl)
    }

    /**
     * Redis `SET NX PX` 기반 분산 락을 획득한 동안 [block]을 실행합니다.
     *
     * 락 키는 Hash 키와 분리되며, [token]과 일치할 때만 Lua 스크립트로 해제합니다.
     * 따라서 서로 다른 Lettuce 연결을 사용하는 호출도 같은 [mapKey] 범위에서
     * read-modify-write 구간을 직렬화할 수 있습니다. [leaseTime]은 호출자가 장애로
     * 중단된 경우를 위한 상한이므로, 블록 실행 시간보다 충분히 길게 설정해야 합니다.
     *
     * @param token 호출마다 새로 생성해야 하는 락 소유 토큰
     * @param leaseTime 락 자동 만료 시간 (기본 1분)
     * @param waitTime 락 획득을 기다리는 최대 시간 (기본 5분)
     * @param block 락을 보유한 상태에서 실행할 작업
     * @return [block]의 반환 값
     * @throws IllegalStateException [waitTime] 안에 락을 획득하지 못한 경우
     */
    fun <R> withDistributedLock(
        token: V,
        leaseTime: Duration = Duration.ofMinutes(1),
        waitTime: Duration = Duration.ofMinutes(5),
        block: () -> R,
    ): R {
        require(!leaseTime.isNegative && !leaseTime.isZero && leaseTime.toMillis() > 0) {
            "leaseTime은 양수여야 합니다."
        }
        require(!waitTime.isNegative) {
            "waitTime은 음수가 될 수 없습니다."
        }

        val lockKey = "$mapKey$LOCK_SUFFIX"
        val deadline = System.nanoTime() + waitTime.toNanos()
        val leaseMillis = leaseTime.toMillis()
        val lockArgs = SetArgs().nx().px(leaseMillis)

        while (syncCommands.set(lockKey, token, lockArgs) == null) {
            if (System.nanoTime() >= deadline) {
                throw IllegalStateException("LettuceMap[$mapKey] 분산 락 획득 시간이 초과되었습니다.")
            }
            LockSupport.parkNanos(LOCK_RETRY_DELAY_NANOS)
        }

        val blockResult = runCatching(block)
        val releaseFailure = runCatching { releaseDistributedLock(lockKey, token) }.exceptionOrNull()
        if (releaseFailure != null) {
            blockResult.exceptionOrNull()?.addSuppressed(releaseFailure)
                ?: throw releaseFailure
        }
        return blockResult.getOrThrow()
    }

    /**
     * 현재 분산 락 토큰을 보유한 경우에만 필드를 쓰기 트랜잭션으로 갱신합니다.
     *
     * `WATCH/MULTI/EXEC`로 락 키를 감시하므로 소유권을 확인한 뒤 lease가 만료되거나
     * 다른 호출자가 락을 인수하면 트랜잭션 전체가 취소됩니다. 따라서 lease 만료 후
     * 이전 호출자가 늦게 도착해 stale 값을 기록하는 것을 막을 수 있습니다.
     *
     * @param field 설정할 필드명
     * @param value 설정할 값
     * @param ttl 적용할 Hash key TTL (null이면 TTL 없음)
     * @param token 현재 분산 락 소유 토큰
     * @return 트랜잭션이 커밋되었으면 true, 락 소유권이 없거나 변경되었으면 false
     */
    fun putTtlIfLockOwned(field: String, value: V, ttl: Duration?, token: V): Boolean =
        executeIfLockOwned(token) {
            if (ttl == null) {
                syncCommands.hset(mapKey, field, value)
            } else if (supportsHSetEx) {
                syncCommands.hsetex(mapKey, HSetExArgs.Builder.ex(ttl), mapOf(field to value))
            } else {
                syncCommands.hset(mapKey, field, value)
                syncCommands.expire(mapKey, ttl)
            }
        }

    /**
     * 현재 분산 락 토큰을 보유한 경우에만 필드를 삭제합니다.
     *
     * @param field 삭제할 필드명
     * @param token 현재 분산 락 소유 토큰
     * @return 트랜잭션이 커밋되었으면 true, 락 소유권이 없거나 변경되었으면 false
     */
    fun removeIfLockOwned(field: String, token: V): Boolean =
        executeIfLockOwned(token) {
            syncCommands.hdel(mapKey, field)
        }

    @Suppress("TooGenericExceptionCaught")
    private fun executeIfLockOwned(token: V, block: () -> Unit): Boolean {
        val lockKey = "$mapKey$LOCK_SUFFIX"
        var transactionStarted = false
        syncCommands.watch(lockKey)
        return try {
            if (!lockTokenMatches(syncCommands.get(lockKey), token)) return false
            syncCommands.multi()
            transactionStarted = true
            block()
            commitWatchedTransaction()
        } catch (error: Exception) {
            if (transactionStarted) runCatching { syncCommands.discard() }
            throw error
        } finally {
            runCatching { syncCommands.unwatch() }
        }
    }

    private fun commitWatchedTransaction(): Boolean {
        val result = syncCommands.exec()
        if (result.wasDiscarded()) return false
        result.forEach { outcome ->
            if (outcome is Throwable) throw outcome
        }
        return true
    }

    private fun lockTokenMatches(actual: V?, expected: V): Boolean =
        when {
            actual is ByteArray && expected is ByteArray -> actual.contentEquals(expected)
            else -> actual == expected
        }

    private fun releaseDistributedLock(lockKey: String, token: V) {
        val released = syncCommands.eval<Long>(
            RELEASE_LOCK_SCRIPT,
            ScriptOutputType.INTEGER,
            arrayOf(lockKey),
            token,
        )
        check(released == 1L) {
            "LettuceMap[$mapKey] 분산 락 해제에 실패했습니다. 토큰이 만료되었거나 소유자가 아닙니다."
        }
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
        if (ttl == null) {
            return put(field, value).also {
                log.debug { "LettuceMap putTtl: mapKey=$mapKey, field=$field, ttl=null" }
            }
        }
        // 개선:
        //  - HSETEX 지원 시: 필드 레벨 TTL 로 기록하고 키 전체에 EXPIRE 를 덮어쓰지 않음
        //    (다른 필드의 TTL/수명을 훼손하지 않도록).
        //  - 미지원 시에만 HSET + EXPIRE 로 키 전체 만료를 설정.
        val added = if (supportsHSetEx) {
            syncCommands.hsetex(mapKey, HSetExArgs.Builder.ex(ttl), mapOf(field to value))
            true
        } else {
            val ok = put(field, value)
            syncCommands.expire(mapKey, ttl)
            ok
        }
        log.debug { "LettuceMap putTtl: mapKey=$mapKey, field=$field, ttl=$ttl, hsetex=$supportsHSetEx" }
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
        // 개선: HSETEX 는 필드-레벨 TTL 이므로 별도 EXPIRE 를 호출하지 않는다.
        //       미지원 시에만 HSET + EXPIRE 로 키 전체 만료 설정.
        if (supportsHSetEx) {
            syncCommands.hsetex(mapKey, HSetExArgs.Builder.ex(ttl), entries)
        } else {
            syncCommands.hset(mapKey, entries)
            syncCommands.expire(mapKey, ttl)
        }
        log.debug { "LettuceMap putAllTtl: mapKey=$mapKey, count=${entries.size}, ttl=$ttl, hsetex=$supportsHSetEx" }
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
