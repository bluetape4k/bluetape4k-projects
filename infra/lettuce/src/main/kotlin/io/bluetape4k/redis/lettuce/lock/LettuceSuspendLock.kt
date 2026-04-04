package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

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
    val defaultLeaseTime: Duration = Duration.ofSeconds(30),
) {
    companion object: KLoggingChannel() {
        private const val RETRY_DELAY_MS = 50L
        private const val DEFAULT_MAX_WAIT_MINUTES = 5L
        private const val UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
    }

    private val tokenRef = atomic<String?>(null)
    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    /**
     * 락이 현재 잠겨 있는지 코루틴으로 확인합니다.
     *
     * @return 잠겨 있으면 true
     */
    suspend fun isLocked(): Boolean = asyncCommands.get(lockKey).await() != null

    /**
     * 현재 인스턴스가 락을 보유하고 있는지 코루틴으로 확인합니다.
     *
     * @return 현재 인스턴스가 락을 보유하면 true
     */
    suspend fun isHeldByCurrentInstance(): Boolean {
        val token = tokenRef.value ?: return false
        return asyncCommands.get(lockKey).await() == token
    }

    /**
     * 지정된 대기 시간 내에 락 획득을 코루틴으로 시도합니다.
     *
     * ```kotlin
     * val lock = LettuceSuspendLock(connection, "my-lock")
     * if (lock.tryLock(waitTime = Duration.ofSeconds(2))) {
     *     try { doWork() } finally { lock.unlock() }
     * }
     * ```
     *
     * @param waitTime 락 획득 대기 시간 (기본값: 즉시 시도)
     * @param leaseTime 락 유지 시간 (기본값: defaultLeaseTime)
     * @return 락 획득 성공 여부
     */
    suspend fun tryLock(
        waitTime: Duration = Duration.ZERO,
        leaseTime: Duration = defaultLeaseTime,
    ): Boolean {
        val token = UUID.randomUUID().toString()
        val leaseMs = leaseTime.toMillis()
        val deadline = System.currentTimeMillis() + waitTime.toMillis()

        do {
            val args = SetArgs().nx().px(leaseMs)
            val result = asyncCommands.set(lockKey, token, args).await()
            if (result != null) {
                tokenRef.value = token
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
     * ```kotlin
     * val lock = LettuceSuspendLock(connection, "my-lock")
     * lock.lock()
     * try { doWork() } finally { lock.unlock() }
     * ```
     *
     * @param leaseTime 락 유지 시간 (기본값: defaultLeaseTime)
     * @param maxWaitTime 최대 대기 시간 (기본값: 5분)
     * @throws IllegalStateException 최대 대기 시간 초과 시
     */
    suspend fun lock(
        leaseTime: Duration = defaultLeaseTime,
        maxWaitTime: Duration = Duration.ofMinutes(DEFAULT_MAX_WAIT_MINUTES),
    ) {
        val token = UUID.randomUUID().toString()
        val leaseMs = leaseTime.toMillis()
        val deadline = System.currentTimeMillis() + maxWaitTime.toMillis()

        while (true) {
            val args = SetArgs().nx().px(leaseMs)
            val result = asyncCommands.set(lockKey, token, args).await()
            if (result != null) {
                tokenRef.value = token
                log.debug { "Lock 획득 성공 (suspend): lockKey=$lockKey" }
                return
            }
            if (System.currentTimeMillis() >= deadline) {
                throw IllegalStateException(
                    "Lock 획득 시간 초과 (suspend): lockKey=$lockKey, maxWaitTime=$maxWaitTime"
                )
            }
            delay(RETRY_DELAY_MS.milliseconds)
        }
    }

    /**
     * 보유 중인 락을 코루틴으로 해제합니다.
     *
     * 토큰을 원자적으로 클리어한 뒤 Lua 스크립트로 해제하여,
     * 네트워크 오류 시에도 재시도로 다른 인스턴스의 락을 해제하는 것을 방지합니다.
     *
     * ```kotlin
     * val lock = LettuceSuspendLock(connection, "my-lock")
     * lock.lock()
     * try { doWork() } finally { lock.unlock() }
     * ```
     *
     * @throws IllegalStateException 락을 보유하지 않은 경우
     */
    suspend fun unlock() {
        val token =
            tokenRef.getAndSet(null)
                ?: throw IllegalStateException("현재 인스턴스가 락을 보유하지 않습니다: lockKey=$lockKey")

        val released =
            asyncCommands
                .eval<Long>(UNLOCK_SCRIPT, ScriptOutputType.INTEGER, arrayOf(lockKey), token)
                .await()

        if (released == 0L) {
            throw IllegalStateException("Lock 해제 실패 (토큰 불일치 또는 만료, suspend): lockKey=$lockKey")
        }
        log.debug { "Lock 해제 성공 (suspend): lockKey=$lockKey" }
    }
}
