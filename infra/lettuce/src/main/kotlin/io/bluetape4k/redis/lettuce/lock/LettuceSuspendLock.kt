package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.awaitSuspending
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Lettuce Redis 클라이언트를 이용한 분산 락의 코루틴 구현체입니다.
 *
 * [LettuceLock]의 코루틴(suspend) 버전으로, `SET NX PX`와 Lua 스크립트를 통해
 * 원자적 락 획득/해제를 suspend 함수로 제공합니다.
 *
 * ```kotlin
 * val lock = LettuceSuspendLock(connection, "my-lock")
 *
 * if (lock.tryLock()) {
 *     try { doWork() } finally { lock.unlock() }
 * }
 * ```
 *
 * @param connection Lettuce StatefulRedisConnection (StringCodec 기반)
 * @param lockKey Redis에 저장될 락 키
 * @param defaultLeaseTime 락 유지 시간 기본값 (기본 30초)
 */
class LettuceSuspendLock(
    private val connection: StatefulRedisConnection<String, String>,
    val lockKey: String,
    val defaultLeaseTime: Duration = 30.seconds,
) {
    companion object: KLogging() {
        private const val RETRY_DELAY_MS = 50L
        private const val UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
    }

    private val tokenRef = AtomicReference<String?>(null)
    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    /**
     * 락이 현재 잠겨 있는지 코루틴으로 확인합니다.
     *
     * @return 잠겨 있으면 true
     */
    suspend fun isLocked(): Boolean =
        asyncCommands.get(lockKey).awaitSuspending() != null

    /**
     * 현재 인스턴스가 락을 보유하고 있는지 코루틴으로 확인합니다.
     *
     * @return 현재 인스턴스가 락을 보유하면 true
     */
    suspend fun isHeldByCurrentInstance(): Boolean {
        val token = tokenRef.get() ?: return false
        return asyncCommands.get(lockKey).awaitSuspending() == token
    }

    /**
     * 지정된 대기 시간 내에 락 획득을 코루틴으로 시도합니다.
     *
     * @param waitTime 락 획득 대기 시간 (기본값: 즉시 시도)
     * @param leaseTime 락 유지 시간 (기본값: defaultLeaseTime)
     * @return 락 획득 성공 여부
     */
    suspend fun tryLock(
        waitTime: Duration = ZERO,
        leaseTime: Duration = defaultLeaseTime,
    ): Boolean {
        val token = UUID.randomUUID().toString()
        val leaseMs = leaseTime.inWholeMilliseconds
        val deadline = System.currentTimeMillis() + waitTime.inWholeMilliseconds

        do {
            val args = SetArgs().nx().px(leaseMs)
            val result = asyncCommands.set(lockKey, token, args).awaitSuspending()
            if (result != null) {
                tokenRef.set(token)
                log.debug { "Lock 획득 성공 (suspend): lockKey=$lockKey" }
                return true
            }
            if (System.currentTimeMillis() < deadline) {
                delay(RETRY_DELAY_MS.milliseconds)
            }
        } while (System.currentTimeMillis() < deadline)

        log.debug { "Lock 획득 실패 (timeout, suspend): lockKey=$lockKey" }
        return false
    }

    /**
     * 락을 획득할 때까지 코루틴으로 대기합니다.
     *
     * @param leaseTime 락 유지 시간 (기본값: defaultLeaseTime)
     */
    suspend fun lock(leaseTime: Duration = defaultLeaseTime) {
        val token = UUID.randomUUID().toString()
        val leaseMs = leaseTime.inWholeMilliseconds

        while (true) {
            val args = SetArgs().nx().px(leaseMs)
            val result = asyncCommands.set(lockKey, token, args).awaitSuspending()
            if (result != null) {
                tokenRef.set(token)
                log.debug { "Lock 획득 성공 (suspend): lockKey=$lockKey" }
                return
            }
            delay(RETRY_DELAY_MS.milliseconds)
        }
    }

    /**
     * 보유 중인 락을 코루틴으로 해제합니다.
     *
     * @throws IllegalStateException 락을 보유하지 않은 경우
     */
    suspend fun unlock() {
        val token = tokenRef.get()
            ?: throw IllegalStateException("현재 인스턴스가 락을 보유하지 않습니다: lockKey=$lockKey")

        val released = asyncCommands.eval<Long>(UNLOCK_SCRIPT, ScriptOutputType.INTEGER, arrayOf(lockKey), token)
            .awaitSuspending()

        if (released == 0L) {
            throw IllegalStateException("Lock 해제 실패 (토큰 불일치 또는 만료, suspend): lockKey=$lockKey")
        }
        tokenRef.set(null)
        log.debug { "Lock 해제 성공 (suspend): lockKey=$lockKey" }
    }
}
