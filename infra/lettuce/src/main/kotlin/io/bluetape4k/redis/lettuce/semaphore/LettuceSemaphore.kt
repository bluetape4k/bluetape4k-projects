package io.bluetape4k.redis.lettuce.semaphore

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.bluetape4k.support.requirePositiveNumber
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport

/**
 * Lettuce Redis 클라이언트를 이용한 분산 세마포어(Distributed Semaphore) 구현체입니다.
 *
 * Redis의 카운터(잔여 허가 수), owner token hash, expiry sorted set을 사용하여 세마포어를 구현합니다.
 * Lua 스크립트를 통해 acquire/release/expired-owner cleanup을 원자적으로 처리합니다.
 *
 * 동기, 비동기(CompletableFuture) 2가지 방식을 지원합니다.
 * 코루틴(suspend) 방식은 [LettuceSuspendSemaphore]를 사용하세요.
 *
 * ```kotlin
 * val semaphore = LettuceSemaphore(connection, "my-semaphore", totalPermits = 3)
 * semaphore.initialize() // 또는 trySetPermits(3)
 *
 * // 동기 방식
 * if (semaphore.tryAcquire()) {
 *     try { doWork() } finally { semaphore.release() }
 * }
 * ```
 *
 * @param connection Lettuce StatefulRedisConnection (StringCodec 기반)
 * @param semaphoreKey Redis에 저장될 세마포어 키
 * @param totalPermits 전체 허가 수
 * @param leaseTime 획득한 permit owner token의 lease time
 */
class LettuceSemaphore(
    private val connection: StatefulRedisConnection<String, String>,
    val semaphoreKey: String,
    val totalPermits: Int,
    private val leaseTime: Duration = Duration.ofSeconds(30),
) {
    companion object: KLogging() {
        private const val RETRY_DELAY_MS = 50L
        private const val RETRY_DELAY_NANOS = RETRY_DELAY_MS * 1_000_000L
    }

    // 개선: getter → final field 로 변경 (매 호출 connection.sync()/async() 호출 제거).
    private val syncCommands: RedisCommands<String, String> = connection.sync()
    private val asyncCommands: RedisAsyncCommands<String, String> = connection.async()
    private val ownersKey = "$semaphoreKey:owners"
    private val expirationsKey = "$semaphoreKey:expirations"
    private val localPermits = LocalSemaphorePermits()
    private val leaseMillis = leaseTime.requirePositiveMillis("leaseTime").toMillis()

    init {
        totalPermits.requirePositiveNumber("totalPermits")
    }

    /**
     * 세마포어를 초기화합니다. (이미 존재하면 무시)
     * `SET semaphoreKey totalPermits NX` 명령을 사용합니다.
     */
    fun initialize() {
        val args = SetArgs().nx()
        syncCommands.set(semaphoreKey, totalPermits.toString(), args)
        log.debug { "세마포어 초기화: semaphoreKey=$semaphoreKey, totalPermits=$totalPermits" }
    }

    /**
     * 허가 수를 강제로 설정합니다.
     *
     * @param permits 설정할 허가 수 (양수여야 합니다)
     * @throws IllegalArgumentException permits가 0 이하인 경우
     */
    fun trySetPermits(permits: Int) {
        permits.requirePositiveNumber("permits")
        syncCommands.set(semaphoreKey, permits.toString())
        syncCommands.del(ownersKey, expirationsKey)
        localPermits.clear()
        log.debug { "세마포어 허가 수 설정: semaphoreKey=$semaphoreKey, permits=$permits" }
    }

    /**
     * 현재 사용 가능한 허가 수를 반환합니다.
     *
     * @return 잔여 허가 수 (초기화 안 된 경우 0)
     */
    fun availablePermits(): Int =
        RedisScriptRunner.run<Long>(
            syncCommands, LettuceSemaphoreScripts.AVAILABLE_SCRIPT, ScriptOutputType.INTEGER,
            semaphoreKeys(), nowMillis().toString(), totalPermits.toString()
        ).toInt()

    // =========================================================================
    // 동기 API
    // =========================================================================

    /**
     * 즉시 허가 획득을 시도합니다.
     *
     * ```kotlin
     * val semaphore = LettuceSemaphore(connection, "my-sem", totalPermits = 3)
     * semaphore.initialize()
     * val acquired = semaphore.tryAcquire()
     * // acquired == true
     * ```
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @return 획득 성공 여부
     */
    fun tryAcquire(permits: Int = 1): Boolean {
        permits.requirePositiveNumber("permits")
        val token = UUID.randomUUID().toString()
        val now = nowMillis()

        val result = RedisScriptRunner.run<Long>(
            syncCommands, LettuceSemaphoreScripts.ACQUIRE_SCRIPT, ScriptOutputType.INTEGER,
            semaphoreKeys(),
            now.toString(),
            totalPermits.toString(),
            permits.toString(),
            token,
            (now + leaseMillis).toString(),
        )
        val acquired = result >= 0
        if (acquired) {
            localPermits.record(token, permits)
        }
        log.debug { "Semaphore tryAcquire: key=$semaphoreKey, permits=$permits, acquired=$acquired" }
        return acquired
    }

    /**
     * 허가를 획득할 때까지 지정된 시간 동안 대기합니다.
     *
     * ```kotlin
     * val semaphore = LettuceSemaphore(connection, "my-sem", totalPermits = 3)
     * semaphore.initialize()
     * semaphore.acquire()  // 허가 획득 대기
     * try {
     *     doWork()
     * } finally {
     *     semaphore.release()
     * }
     * ```
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @param waitTime 최대 대기 시간 (기본값: 30초)
     * @throws IllegalStateException 지정된 시간 내에 허가를 획득하지 못한 경우
     */
    fun acquire(permits: Int = 1, waitTime: Duration = Duration.ofSeconds(30)) {
        permits.requirePositiveNumber("permits")

        val deadline = System.currentTimeMillis() + waitTime.toMillis()
        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire(permits)) return
            // LockSupport.parkNanos: Thread.sleep 과 달리 Virtual Thread carrier thread 를 핀닝하지 않음
            LockSupport.parkNanos(RETRY_DELAY_NANOS)
        }
        throw IllegalStateException("세마포어 획득 시간 초과: semaphoreKey=$semaphoreKey, permits=${permits}")
    }

    /**
     * 허가를 반납합니다.
     *
     * ```kotlin
     * semaphore.acquire()
     * try {
     *     doWork()
     * } finally {
     *     semaphore.release()
     *     val remaining = semaphore.availablePermits()
     *     // remaining == totalPermits (반납 후 복원)
     * }
     * ```
     *
     * @param permits 반납할 허가 수 (기본값: 1)
     */
    fun release(permits: Int = 1) {
        permits.requirePositiveNumber("permits")

        val releases = localPermits.select(permits)
        releases.forEach { release ->
            val remaining = releaseOwnedPermitsSync(release)
            log.debug { "Semaphore release: key=$semaphoreKey, permits=${release.permits}, remaining=$remaining" }
        }
    }

    // =========================================================================
    // 비동기 API (CompletableFuture)
    // =========================================================================

    /**
     * 즉시 허가 획득을 비동기로 시도합니다.
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @return 획득 성공 여부를 담은 CompletableFuture
     */
    fun tryAcquireAsync(permits: Int = 1): CompletableFuture<Boolean> {
        permits.requirePositiveNumber("permits")
        val token = UUID.randomUUID().toString()
        val now = nowMillis()

        return RedisScriptRunner.runAsync<Long>(
            asyncCommands, LettuceSemaphoreScripts.ACQUIRE_SCRIPT, ScriptOutputType.INTEGER,
            semaphoreKeys(),
            now.toString(),
            totalPermits.toString(),
            permits.toString(),
            token,
            (now + leaseMillis).toString(),
        ).thenApply { result ->
            val acquired = result >= 0
            if (acquired) {
                localPermits.record(token, permits)
            }
            log.debug { "Semaphore tryAcquireAsync: key=$semaphoreKey, permits=$permits, acquired=$acquired" }
            acquired
        }
    }

    /**
     * 허가를 획득할 때까지 비동기로 대기합니다.
     *
     * @param permits 획득할 허가 수 (기본값: 1)
     * @param waitTime 최대 대기 시간 (기본값: 30초)
     * @return 완료를 나타내는 CompletableFuture
     */
    fun acquireAsync(permits: Int = 1, waitTime: Duration = Duration.ofSeconds(30)): CompletableFuture<Unit> {
        permits.requirePositiveNumber("permits")
        val deadline = System.currentTimeMillis() + waitTime.toMillis()

        fun attempt(): CompletableFuture<Unit> =
            tryAcquireAsync(permits).thenCompose { acquired ->
                if (acquired) {
                    CompletableFuture.completedFuture(Unit)
                } else if (System.currentTimeMillis() < deadline) {
                    // Thread.sleep 대신 delayedExecutor 를 사용하여 스레드 풀을 차단하지 않음
                    val delayed = CompletableFuture.delayedExecutor(RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
                    CompletableFuture.runAsync({}, delayed).thenCompose { attempt() }
                } else {
                    CompletableFuture.failedFuture(
                        IllegalStateException("세마포어 획득 시간 초과 (async): semaphoreKey=$semaphoreKey")
                    )
                }
            }

        return attempt()
    }

    /**
     * 허가를 비동기로 반납합니다.
     *
     * @param permits 반납할 허가 수 (기본값: 1)
     * @return 완료를 나타내는 CompletableFuture
     */
    fun releaseAsync(permits: Int = 1): CompletableFuture<Unit> {
        permits.requirePositiveNumber("permits")

        val releases = localPermits.select(permits)
        var future = CompletableFuture.completedFuture(Unit)
        releases.forEach { release ->
            future = future.thenCompose {
                RedisScriptRunner.runAsync<Long>(
                    asyncCommands, LettuceSemaphoreScripts.RELEASE_SCRIPT, ScriptOutputType.INTEGER,
                    semaphoreKeys(),
                    nowMillis().toString(),
                    totalPermits.toString(),
                    release.permits.toString(),
                    release.token,
                ).thenApply { remaining ->
                    handleReleaseResult(release, remaining)
                    log.debug { "Semaphore releaseAsync: key=$semaphoreKey, permits=${release.permits}, remaining=$remaining" }
                }
            }
        }
        return future
    }

    private fun releaseOwnedPermitsSync(release: PermitRelease): Long {
        val remaining = RedisScriptRunner.run<Long>(
            syncCommands, LettuceSemaphoreScripts.RELEASE_SCRIPT, ScriptOutputType.INTEGER,
            semaphoreKeys(),
            nowMillis().toString(),
            totalPermits.toString(),
            release.permits.toString(),
            release.token,
        )
        handleReleaseResult(release, remaining)
        return remaining
    }

    private fun handleReleaseResult(release: PermitRelease, remaining: Long) {
        when {
            remaining >= 0L -> localPermits.markReleased(release)
            remaining == -1L -> {
                localPermits.markLost(release)
                error("Semaphore permits are no longer owned or already expired: semaphoreKey=$semaphoreKey")
            }
            else -> error("Semaphore release exceeds owned permits: semaphoreKey=$semaphoreKey")
        }
    }

    private fun semaphoreKeys(): Array<String> = arrayOf(semaphoreKey, ownersKey, expirationsKey)

    private fun nowMillis(): Long = System.currentTimeMillis()
}
