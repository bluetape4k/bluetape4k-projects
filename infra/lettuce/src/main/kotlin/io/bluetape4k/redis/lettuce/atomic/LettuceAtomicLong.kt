package io.bluetape4k.redis.lettuce.atomic

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import java.util.concurrent.CompletableFuture

/**
 * Lettuce Redis 클라이언트를 이용한 분산 AtomicLong 구현체입니다.
 *
 * Redisson의 [RAtomicLong]을 참고하여 Lettuce 기반으로 구현하였습니다.
 * Redis String 타입에 Long 값을 저장하고, INCR/DECR/INCRBY 명령과 Lua 스크립트를 통해
 * 원자적인 연산을 제공합니다.
 *
 * 동기, 비동기(CompletableFuture) 2가지 방식을 지원합니다.
 * 코루틴(suspend) 방식은 [LettuceSuspendAtomicLong]을 사용하세요.
 *
 * ```kotlin
 * val counter = LettuceAtomicLong(connection, "my-counter", initialValue = 0L)
 *
 * // 동기 방식
 * counter.incrementAndGet()  // 1
 * counter.addAndGet(5)       // 6
 * ```
 *
 * @param connection Lettuce StatefulRedisConnection (StringCodec 기반)
 * @param key Redis에 저장될 키
 * @param initialValue 초기값 (키가 없을 경우에만 설정)
 */
class LettuceAtomicLong(
    private val connection: StatefulRedisConnection<String, String>,
    val key: String,
    val initialValue: Long = 0L,
) {
    companion object: KLogging() {
        /**
         * Lua: GET and SET (원자적)
         * KEYS[1]=key, ARGV[1]=newValue
         * 반환: 이전 값 (없으면 "0")
         */
        private const val GET_AND_SET_SCRIPT = """
local old = redis.call('get', KEYS[1])
redis.call('set', KEYS[1], ARGV[1])
if old then return old else return '0' end"""

        /**
         * Lua: GET and ADD (원자적)
         * KEYS[1]=key, ARGV[1]=delta
         * 반환: 이전 값 (없으면 "0")
         */
        private const val GET_AND_ADD_SCRIPT = """
local old = tonumber(redis.call('get', KEYS[1])) or 0
redis.call('incrby', KEYS[1], ARGV[1])
return tostring(old)"""

        /**
         * Lua: Compare and Set (원자적)
         * KEYS[1]=key, ARGV[1]=expect, ARGV[2]=update
         * 반환: 1 (성공), 0 (실패)
         */
        private const val COMPARE_AND_SET_SCRIPT = """
local current = redis.call('get', KEYS[1])
if (current == false and ARGV[1] == '0') or current == ARGV[1] then
  redis.call('set', KEYS[1], ARGV[2])
  return 1
else
  return 0
end"""
    }

    private val syncCommands: RedisCommands<String, String> get() = connection.sync()
    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    init {
        // 키가 없을 경우에만 초기값 설정
        syncCommands.set(key, initialValue.toString(), SetArgs().nx())
    }

    // =========================================================================
    // 동기 API
    // =========================================================================

    /**
     * 현재 값을 반환합니다.
     *
     * ```kotlin
     * val counter = LettuceAtomicLong(connection, "my-counter", initialValue = 5L)
     * val value = counter.get()
     * // value == 5
     * ```
     *
     * @return 현재 Long 값
     */
    fun get(): Long = syncCommands.get(key)?.toLong() ?: initialValue

    /**
     * 값을 설정합니다.
     *
     * ```kotlin
     * val counter = LettuceAtomicLong(connection, "my-counter")
     * counter.set(42L)
     * val value = counter.get()
     * // value == 42
     * ```
     *
     * @param value 설정할 값
     */
    fun set(value: Long) {
        syncCommands.set(key, value.toString())
        log.debug { "LettuceAtomicLong set: key=$key, value=$value" }
    }

    /**
     * 현재 값을 반환하고 새 값으로 설정합니다.
     *
     * @param value 새로운 값
     * @return 이전 값
     */
    fun getAndSet(value: Long): Long {
        val result = syncCommands.eval<String>(
            GET_AND_SET_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), value.toString()
        )
        return result?.toLongOrNull() ?: 0L
    }

    /**
     * 값을 1 증가시키고 증가된 값을 반환합니다.
     *
     * ```kotlin
     * val counter = LettuceAtomicLong(connection, "my-counter", initialValue = 0L)
     * val v1 = counter.incrementAndGet()
     * // v1 == 1
     * val v2 = counter.incrementAndGet()
     * // v2 == 2
     * ```
     *
     * @return 증가된 값
     */
    fun incrementAndGet(): Long = syncCommands.incr(key) ?: 1L

    /**
     * 값을 1 감소시키고 감소된 값을 반환합니다.
     *
     * ```kotlin
     * val counter = LettuceAtomicLong(connection, "my-counter", initialValue = 5L)
     * val v = counter.decrementAndGet()
     * // v == 4
     * ```
     *
     * @return 감소된 값
     */
    fun decrementAndGet(): Long = syncCommands.decr(key) ?: -1L

    /**
     * 값에 delta를 더하고 더해진 값을 반환합니다.
     *
     * ```kotlin
     * val counter = LettuceAtomicLong(connection, "my-counter", initialValue = 0L)
     * val v = counter.addAndGet(5L)
     * // v == 5
     * ```
     *
     * @param delta 더할 값
     * @return 더해진 값
     */
    fun addAndGet(delta: Long): Long = syncCommands.incrby(key, delta) ?: delta

    /**
     * 현재 값을 반환하고 1 증가시킵니다.
     *
     * @return 증가 전 값
     */
    fun getAndIncrement(): Long {
        val result = syncCommands.eval<String>(
            GET_AND_ADD_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), "1"
        )
        return result?.toLongOrNull() ?: 0L
    }

    /**
     * 현재 값을 반환하고 1 감소시킵니다.
     *
     * @return 감소 전 값
     */
    fun getAndDecrement(): Long {
        val result = syncCommands.eval<String>(
            GET_AND_ADD_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), "-1"
        )
        return result?.toLongOrNull() ?: 0L
    }

    /**
     * 현재 값을 반환하고 delta를 더합니다.
     *
     * @param delta 더할 값
     * @return 더하기 전 값
     */
    fun getAndAdd(delta: Long): Long {
        val result = syncCommands.eval<String>(
            GET_AND_ADD_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), delta.toString()
        )
        return result?.toLongOrNull() ?: 0L
    }

    /**
     * 현재 값이 expect와 같으면 update로 변경합니다.
     *
     * @param expect 예상 값
     * @param update 새로운 값
     * @return 변경 성공 여부
     */
    fun compareAndSet(expect: Long, update: Long): Boolean {
        val result = syncCommands.eval<Long>(
            COMPARE_AND_SET_SCRIPT, ScriptOutputType.INTEGER,
            arrayOf(key), expect.toString(), update.toString()
        )
        return result == 1L
    }

    // =========================================================================
    // 비동기 API (CompletableFuture)
    // =========================================================================

    /** 현재 값을 비동기로 반환합니다. */
    fun getAsync(): CompletableFuture<Long> =
        asyncCommands.get(key).toCompletableFuture()
            .thenApply { it?.toLongOrNull() ?: initialValue }

    /** 값을 비동기로 설정합니다. */
    fun setAsync(value: Long): CompletableFuture<Unit> =
        asyncCommands.set(key, value.toString()).toCompletableFuture()
            .thenApply { log.debug { "LettuceAtomicLong setAsync: key=$key, value=$value" } }

    /** 현재 값을 반환하고 새 값으로 설정합니다 (비동기). */
    fun getAndSetAsync(value: Long): CompletableFuture<Long> =
        asyncCommands.eval<String>(
            GET_AND_SET_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), value.toString()
        ).toCompletableFuture().thenApply { it?.toLongOrNull() ?: 0L }

    /** 값을 1 증가시키고 증가된 값을 반환합니다 (비동기). */
    fun incrementAndGetAsync(): CompletableFuture<Long> =
        asyncCommands.incr(key).toCompletableFuture()
            .thenApply { it ?: 1L }

    /** 값을 1 감소시키고 감소된 값을 반환합니다 (비동기). */
    fun decrementAndGetAsync(): CompletableFuture<Long> =
        asyncCommands.decr(key).toCompletableFuture()
            .thenApply { it ?: -1L }

    /** 값에 delta를 더하고 더해진 값을 반환합니다 (비동기). */
    fun addAndGetAsync(delta: Long): CompletableFuture<Long> =
        asyncCommands.incrby(key, delta).toCompletableFuture()
            .thenApply { it ?: delta }

    /** 현재 값을 반환하고 1 증가시킵니다 (비동기). */
    fun getAndIncrementAsync(): CompletableFuture<Long> =
        asyncCommands.eval<String>(
            GET_AND_ADD_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), "1"
        ).toCompletableFuture().thenApply { it?.toLongOrNull() ?: 0L }

    /** 현재 값을 반환하고 1 감소시킵니다 (비동기). */
    fun getAndDecrementAsync(): CompletableFuture<Long> =
        asyncCommands.eval<String>(
            GET_AND_ADD_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), "-1"
        ).toCompletableFuture().thenApply { it?.toLongOrNull() ?: 0L }

    /** 현재 값을 반환하고 delta를 더합니다 (비동기). */
    fun getAndAddAsync(delta: Long): CompletableFuture<Long> =
        asyncCommands.eval<String>(
            GET_AND_ADD_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), delta.toString()
        ).toCompletableFuture().thenApply { it?.toLongOrNull() ?: 0L }

    /** 현재 값이 expect와 같으면 update로 변경합니다 (비동기). */
    fun compareAndSetAsync(expect: Long, update: Long): CompletableFuture<Boolean> =
        asyncCommands.eval<Long>(
            COMPARE_AND_SET_SCRIPT, ScriptOutputType.INTEGER,
            arrayOf(key), expect.toString(), update.toString()
        ).toCompletableFuture().thenApply { it == 1L }

}
