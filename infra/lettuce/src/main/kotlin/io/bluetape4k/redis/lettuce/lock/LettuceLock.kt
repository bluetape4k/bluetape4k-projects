package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Lettuce Redis 클라이언트를 이용한 분산 락(Distributed Lock) 구현체입니다.
 *
 * Redisson의 RLock을 참고하여 Lettuce 기반으로 구현한 비재진입(non-reentrant) 분산 뮤텍스입니다.
 * 락 토큰으로 UUID를 사용하여 스레드/코루틴에 독립적으로 동작합니다.
 *
 * 동기, 비동기(CompletableFuture) 2가지 방식을 지원합니다.
 * 코루틴(suspend) 방식은 [LettuceSuspendLock]을 사용하세요.
 *
 * ```kotlin
 * val lock = LettuceLock(connection, "my-lock")
 *
 * // 동기 방식
 * if (lock.tryLock()) {
 *     try { doWork() } finally { lock.unlock() }
 * }
 * ```
 *
 * @param connection Lettuce StatefulRedisConnection (StringCodec 기반)
 * @param lockKey Redis에 저장될 락 키
 * @param defaultLeaseTime 락 유지 시간 기본값 (기본 30초)
 */
class LettuceLock(
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
    private val syncCommands: RedisCommands<String, String> get() = connection.sync()
    private val asyncCommands: RedisAsyncCommands<String, String> get() = connection.async()

    /**
     * 락이 현재 잠겨 있는지 확인합니다.
     */
    fun isLocked(): Boolean = syncCommands.get(lockKey) != null

    /**
     * 현재 인스턴스가 락을 보유하고 있는지 Redis에서 실제로 확인합니다.
     *
     * 로컬 토큰과 Redis에 저장된 값을 비교하여 검증합니다.
     * 리스 시간(leaseTime) 만료로 Redis에서 키가 삭제된 경우 false를 반환합니다.
     */
    fun isHeldByCurrentInstance(): Boolean {
        val token = tokenRef.get() ?: return false
        return syncCommands.get(lockKey) == token
    }

    // =========================================================================
    // 동기 API
    // =========================================================================

    /**
     * 지정된 대기 시간 내에 락 획득을 시도합니다.
     *
     * @param waitTime 락 획득 대기 시간 (기본값: 즉시 시도)
     * @param leaseTime 락 유지 시간 (기본값: defaultLeaseTime)
     * @return 락 획득 성공 여부
     */
    fun tryLock(
        waitTime: Duration = ZERO,
        leaseTime: Duration = defaultLeaseTime,
    ): Boolean {
        val token = UUID.randomUUID().toString()
        val leaseMs = leaseTime.inWholeMilliseconds
        val deadline = System.currentTimeMillis() + waitTime.inWholeMilliseconds

        do {
            val args = SetArgs().nx().px(leaseMs)
            val result = syncCommands.set(lockKey, token, args)
            if (result != null) {
                tokenRef.set(token)
                log.debug { "Lock 획득 성공: lockKey=$lockKey" }
                return true
            }
            if (System.currentTimeMillis() < deadline) {
                Thread.sleep(RETRY_DELAY_MS)
            }
        } while (System.currentTimeMillis() < deadline)

        log.debug { "Lock 획득 실패 (timeout): lockKey=$lockKey" }
        return false
    }

    /**
     * 락을 획득할 때까지 블로킹합니다.
     *
     * @param leaseTime 락 유지 시간 (기본값: defaultLeaseTime)
     * @throws InterruptedException 대기 중 인터럽트 발생 시
     */
    fun lock(leaseTime: Duration = defaultLeaseTime) {
        val token = UUID.randomUUID().toString()
        val leaseMs = leaseTime.inWholeMilliseconds

        while (true) {
            val args = SetArgs().nx().px(leaseMs)
            val result = syncCommands.set(lockKey, token, args)
            if (result != null) {
                tokenRef.set(token)
                log.debug { "Lock 획득 성공: lockKey=$lockKey" }
                return
            }
            Thread.sleep(RETRY_DELAY_MS)
        }
    }

    /**
     * 보유 중인 락을 해제합니다.
     *
     * @throws IllegalStateException 락을 보유하지 않은 경우
     */
    fun unlock() {
        val token = tokenRef.getAndSet(null)
            ?: throw IllegalStateException("현재 인스턴스가 락을 보유하지 않습니다: lockKey=$lockKey")

        val released = syncCommands.eval<Long>(UNLOCK_SCRIPT, ScriptOutputType.INTEGER, arrayOf(lockKey), token)
        if (released == 0L) {
            throw IllegalStateException("Lock 해제 실패 (토큰 불일치 또는 만료): lockKey=$lockKey")
        }
        log.debug { "Lock 해제 성공: lockKey=$lockKey" }
    }

    // =========================================================================
    // 비동기 API (CompletableFuture)
    // =========================================================================

    /**
     * 지정된 대기 시간 내에 락 획득을 비동기로 시도합니다.
     *
     * @param waitTime 락 획득 대기 시간 (기본값: 즉시 시도)
     * @param leaseTime 락 유지 시간 (기본값: defaultLeaseTime)
     * @return 락 획득 성공 여부를 담은 CompletableFuture
     */
    fun tryLockAsync(
        waitTime: Duration = ZERO,
        leaseTime: Duration = defaultLeaseTime,
    ): CompletableFuture<Boolean> {
        val token = UUID.randomUUID().toString()
        val leaseMs = leaseTime.inWholeMilliseconds
        val deadline = System.currentTimeMillis() + waitTime.inWholeMilliseconds

        fun attempt(): CompletableFuture<Boolean> {
            val args = SetArgs().nx().px(leaseMs)
            return asyncCommands.set(lockKey, token, args).toCompletableFuture()
                .thenCompose { result ->
                    if (result != null) {
                        tokenRef.set(token)
                        log.debug { "Lock 획득 성공 (async): lockKey=$lockKey" }
                        CompletableFuture.completedFuture(true)
                    } else if (System.currentTimeMillis() < deadline) {
                        // Thread.sleep 대신 delayedExecutor 를 사용하여 스레드 풀을 차단하지 않음
                        val delayed = CompletableFuture.delayedExecutor(RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
                        CompletableFuture.runAsync({}, delayed).thenCompose { attempt() }
                    } else {
                        log.debug { "Lock 획득 실패 (timeout, async): lockKey=$lockKey" }
                        CompletableFuture.completedFuture(false)
                    }
                }
        }

        return attempt()
    }

    /**
     * 락을 획득할 때까지 비동기로 대기합니다.
     *
     * @param leaseTime 락 유지 시간 (기본값: defaultLeaseTime)
     * @param maxWaitTime 최대 대기 시간 (기본값: 5분). 이 시간을 초과하면 예외 발생.
     * @return 완료를 나타내는 CompletableFuture
     * @throws IllegalStateException maxWaitTime 초과 시
     */
    fun lockAsync(
        leaseTime: Duration = defaultLeaseTime,
        maxWaitTime: Duration = 5.minutes,
    ): CompletableFuture<Unit> {
        val token = UUID.randomUUID().toString()
        val leaseMs = leaseTime.inWholeMilliseconds
        val deadline = System.currentTimeMillis() + maxWaitTime.inWholeMilliseconds

        fun attempt(): CompletableFuture<Unit> {
            val args = SetArgs().nx().px(leaseMs)
            return asyncCommands.set(lockKey, token, args).toCompletableFuture()
                .thenCompose { result ->
                    if (result != null) {
                        tokenRef.set(token)
                        log.debug { "Lock 획득 성공 (async): lockKey=$lockKey" }
                        CompletableFuture.completedFuture(Unit)
                    } else if (System.currentTimeMillis() < deadline) {
                        val delayed = CompletableFuture.delayedExecutor(RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
                        CompletableFuture.runAsync({}, delayed).thenCompose { attempt() }
                    } else {
                        CompletableFuture.failedFuture(
                            IllegalStateException("Lock 획득 시간 초과 (async): lockKey=$lockKey")
                        )
                    }
                }
        }

        return attempt()
    }

    /**
     * 보유 중인 락을 비동기로 해제합니다.
     *
     * @return 완료를 나타내는 CompletableFuture
     */
    fun unlockAsync(): CompletableFuture<Unit> {
        val token = tokenRef.getAndSet(null)
            ?: return CompletableFuture.failedFuture(
                IllegalStateException("현재 인스턴스가 락을 보유하지 않습니다: lockKey=$lockKey")
            )

        return asyncCommands.eval<Long>(UNLOCK_SCRIPT, ScriptOutputType.INTEGER, arrayOf(lockKey), token)
            .toCompletableFuture()
            .thenApply { released ->
                if (released == 0L) {
                    throw IllegalStateException("Lock 해제 실패 (토큰 불일치 또는 만료, async): lockKey=$lockKey")
                }
                log.debug { "Lock 해제 성공 (async): lockKey=$lockKey" }
            }
    }

}
