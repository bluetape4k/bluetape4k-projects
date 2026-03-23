package io.bluetape4k.redis.lettuce.atomic

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await

/**
 * Lettuce Redis 클라이언트를 이용한 분산 AtomicLong의 코루틴 구현체입니다.
 *
 * [LettuceAtomicLong]의 코루틴(suspend) 버전으로, Redis String 타입에 Long 값을 저장하고
 * INCR/DECR/INCRBY 명령과 Lua 스크립트를 통해 원자적 연산을 suspend 함수로 제공합니다.
 *
 * ```kotlin
 * val counter = LettuceSuspendAtomicLong(connection, "my-counter", initialValue = 0L)
 * counter.incrementAndGet()  // 1
 * counter.addAndGet(5)       // 6
 * ```
 *
 * @param connection Lettuce StatefulRedisConnection (StringCodec 기반)
 * @param key Redis에 저장될 키
 * @param initialValue 초기값 (키가 없을 경우에만 설정)
 *
 * **주의**: 생성자에서 `connection.sync().set()`을 한 번 호출하여 초기값을 설정합니다.
 * 이는 [LettuceAtomicLong]과 동일한 패턴으로, 코루틴 컨텍스트 밖에서 객체를 생성하는 것이 권장됩니다.
 */
class LettuceSuspendAtomicLong(
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

    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    init {
        // 키가 없을 경우에만 초기값 설정
        connection.sync().set(key, initialValue.toString(), SetArgs().nx())
    }

    /**
     * 현재 값을 반환합니다.
     *
     * @return 현재 Long 값
     */
    suspend fun get(): Long =
        asyncCommands.get(key).await()?.toLongOrNull() ?: initialValue

    /**
     * 값을 설정합니다.
     *
     * @param value 설정할 값
     */
    suspend fun set(value: Long) {
        asyncCommands.set(key, value.toString()).await()
        log.debug { "LettuceSuspendAtomicLong set: key=$key, value=$value" }
    }

    /**
     * 현재 값을 반환하고 새 값으로 설정합니다.
     *
     * @param value 새로운 값
     * @return 이전 값
     */
    suspend fun getAndSet(value: Long): Long =
        asyncCommands.eval<String>(
            GET_AND_SET_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), value.toString()
        ).await()?.toLongOrNull() ?: 0L

    /**
     * 값을 1 증가시키고 증가된 값을 반환합니다.
     *
     * @return 증가된 값
     */
    suspend fun incrementAndGet(): Long =
        asyncCommands.incr(key).await() ?: 1L

    /**
     * 값을 1 감소시키고 감소된 값을 반환합니다.
     *
     * @return 감소된 값
     */
    suspend fun decrementAndGet(): Long =
        asyncCommands.decr(key).await() ?: -1L

    /**
     * 값에 delta를 더하고 더해진 값을 반환합니다.
     *
     * @param delta 더할 값
     * @return 더해진 값
     */
    suspend fun addAndGet(delta: Long): Long =
        asyncCommands.incrby(key, delta).await() ?: delta

    /**
     * 현재 값을 반환하고 1 증가시킵니다.
     *
     * @return 증가 전 값
     */
    suspend fun getAndIncrement(): Long =
        asyncCommands.eval<String>(
            GET_AND_ADD_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), "1"
        ).await()?.toLongOrNull() ?: 0L

    /**
     * 현재 값을 반환하고 1 감소시킵니다.
     *
     * @return 감소 전 값
     */
    suspend fun getAndDecrement(): Long =
        asyncCommands.eval<String>(
            GET_AND_ADD_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), "-1"
        ).await()?.toLongOrNull() ?: 0L

    /**
     * 현재 값을 반환하고 delta를 더합니다.
     *
     * @param delta 더할 값
     * @return 더하기 전 값
     */
    suspend fun getAndAdd(delta: Long): Long =
        asyncCommands.eval<String>(
            GET_AND_ADD_SCRIPT, ScriptOutputType.VALUE,
            arrayOf(key), delta.toString()
        ).await()?.toLongOrNull() ?: 0L

    /**
     * 현재 값이 expect와 같으면 update로 변경합니다.
     *
     * @param expect 예상 값
     * @param update 새로운 값
     * @return 변경 성공 여부
     */
    suspend fun compareAndSet(expect: Long, update: Long): Boolean =
        asyncCommands.eval<Long>(
            COMPARE_AND_SET_SCRIPT, ScriptOutputType.INTEGER,
            arrayOf(key), expect.toString(), update.toString()
        ).await() == 1L
}
