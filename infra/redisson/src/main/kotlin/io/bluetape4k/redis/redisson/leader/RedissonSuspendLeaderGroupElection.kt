package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.leader.LeaderGroupState
import io.bluetape4k.leader.coroutines.SuspendLeaderGroupElection
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.future.await
import org.redisson.api.RSemaphore
import org.redisson.api.RedissonClient
import org.redisson.client.RedisException
import java.time.Duration

/**
 * Redisson 분산 Semaphore를 이용하여 복수 리더 선출을 통한 suspend 작업을 수행합니다.
 *
 * ```kotlin
 * val client: RedissonClient = ...
 * val options = RedissonLeaderGroupElectionOptions(maxLeaders = 3)
 * val result: Int = client.runSuspendIfLeaderGroup("batch-job", options) {
 *     // 최대 3개 프로세스가 동시에 실행
 *     delay(100)
 *     42
 * }
 * ```
 *
 * @param lockName 락 이름
 * @param options 리더 선출 옵션
 * @param action 리더 그룹 슬롯 획득 시 수행할 suspend 작업
 * @return 작업 결과
 */
suspend fun <T> RedissonClient.runSuspendIfLeaderGroup(
    lockName: String,
    options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default,
    action: suspend () -> T,
): T {
    lockName.requireNotBlank("lockName")
    options.maxLeaders.requirePositiveNumber("maxLeaders")
    return RedissonSuspendLeaderGroupElection(this, options).runIfLeader(lockName, action)
}


/**
 * Redisson 분산 [RSemaphore]를 이용한 코루틴 기반 복수 리더 선출 구현체입니다.
 *
 * ## 동작
 * - `lockName`별로 Redis 분산 `RSemaphore(maxLeaders)`를 생성하여 동시 실행 수를 제한합니다.
 * - 슬롯이 가득 찬 경우 [LeaderGroupElectionOptions.waitTime] 내에 슬롯을 획득하지 못하면
 *   [RedisException]을 던집니다.
 * - `tryAcquireAsync`/`releaseAsync`를 사용하여 호출 코루틴을 블로킹하지 않습니다.
 * - `action` 예외 발생 시에도 슬롯은 반드시 반환됩니다.
 * - 여러 JVM 프로세스에 걸친 분산 동시 실행 제한에 적합합니다.
 *
 * ## [RedissonLeaderGroupElection] 과의 차이
 * - [RedissonLeaderGroupElection]은 스레드를 블로킹합니다.
 * - 이 구현체는 `awit()`으로 코루틴을 suspend합니다.
 *
 * ```kotlin
 * val options = RedissonLeaderGroupElectionOptions(maxLeaders = 3)
 * val election = RedissonSuspendLeaderGroupElection(redissonClient, options)
 *
 * // 최대 3개 코루틴/프로세스가 동시에 실행
 * val result = election.runIfLeader("batch-job") { processChunkSuspend() }
 *
 * // 상태 조회
 * println(election.state("batch-job"))
 * ```
 *
 * @param redissonClient Redisson 클라이언트
 * @param options 리더 선출 옵션 (maxLeader, waitTime, leaseTime)
 */
class RedissonSuspendLeaderGroupElection private constructor(
    private val redissonClient: RedissonClient,
    options: LeaderGroupElectionOptions,
): SuspendLeaderGroupElection {

    companion object: KLoggingChannel() {
        /**
         * [RedissonSuspendLeaderGroupElection] 인스턴스를 생성합니다.
         *
         * @param redissonClient Redisson 클라이언트
         * @param options 리더 선출 옵션 (maxLeader, waitTime, leaseTime)
         */
        operator fun invoke(
            redissonClient: RedissonClient,
            options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default,
        ): RedissonSuspendLeaderGroupElection {
            options.maxLeaders.requirePositiveNumber("maxLeaders")
            return RedissonSuspendLeaderGroupElection(redissonClient, options)
        }
    }

    override val maxLeaders: Int = options.maxLeaders
    private val waitTime: Duration = options.waitTime

    private fun getInitializedSemaphore(lockName: String): RSemaphore {
        lockName.requireNotBlank("lockName")
        val semaphore = redissonClient.getSemaphore(lockName)
        semaphore.trySetPermits(maxLeaders)
        return semaphore
    }

    private suspend fun getInitializedSemaphoreAsync(lockName: String): RSemaphore {
        lockName.requireNotBlank("lockName")
        val semaphore = redissonClient.getSemaphore(lockName)
        semaphore.trySetPermitsAsync(maxLeaders).await()
        return semaphore
    }

    /**
     * [lockName]에 대해 현재 활성(실행 중인) 리더 수를 반환합니다.
     *
     * `maxLeaders - availablePermits()`로 계산하므로 근사값입니다.
     */
    override fun activeCount(lockName: String): Int =
        maxLeaders - getInitializedSemaphore(lockName).availablePermits()

    /**
     * [lockName]에 대해 새 리더를 수용할 수 있는 남은 슬롯 수를 반환합니다.
     */
    override fun availableSlots(lockName: String): Int =
        getInitializedSemaphore(lockName).availablePermits()

    /**
     * [lockName]에 대한 현재 [LeaderGroupState] 스냅샷을 반환합니다.
     */
    override fun state(lockName: String): LeaderGroupState =
        LeaderGroupState(lockName, maxLeaders, activeCount(lockName))

    /**
     * [lockName]의 분산 [RSemaphore] 슬롯을 비동기로 획득하고 suspend [action]을 실행합니다.
     *
     * - 슬롯이 가득 찬 경우 [waitTime] 내 슬롯을 획득하지 못하면 [RedisException]을 던집니다.
     * - [action] 예외 발생 시에도 슬롯은 반드시 반환됩니다.
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param action 슬롯 획득 성공 시 실행할 suspend 작업
     * @return [action] 실행 결과
     * @throws RedisException 슬롯 획득 실패 또는 인터럽트 발생 시
     */
    override suspend fun <T> runIfLeader(lockName: String, action: suspend () -> T): T {
        lockName.requireNotBlank("lockName")

        val semaphore = getInitializedSemaphoreAsync(lockName)
        log.debug { "리더 그룹 슬롯 획득을 요청합니다. lockName=$lockName, maxLeaders=$maxLeaders" }

        val acquired = try {
            semaphore.tryAcquireAsync(waitTime).await()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn(e) { "슬롯 획득 대기 중 인터럽트가 발생했습니다. lockName=$lockName" }
            throw RedisException("Interrupted while acquiring semaphore slot. lockName=$lockName", e)
        }

        if (!acquired) {
            throw RedisException("Fail to acquire semaphore slot within waitTime. lockName=$lockName")
        }

        log.debug { "리더 그룹 슬롯을 획득하여 suspend 작업을 수행합니다. lockName=$lockName" }
        try {
            return action()
        } finally {
            runCatching { semaphore.releaseAsync().await() }
            log.debug { "작업이 완료되어 슬롯을 반납했습니다. lockName=$lockName" }
        }
    }
}
