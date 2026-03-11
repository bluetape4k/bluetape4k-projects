package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.leader.LeaderGroupElection
import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.leader.LeaderGroupState
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import org.redisson.api.RSemaphore
import org.redisson.api.RedissonClient
import org.redisson.client.RedisException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * Redisson 분산 Semaphore를 이용하여 복수 리더 선출을 통한 작업을 수행합니다.
 *
 * ```kotlin
 * val client: RedissonClient = ...
 * val options = RedissonLeaderGroupElectionOptions(maxLeaders = 3)
 * val result: Int = client.runIfLeaderGroup("batch-job", options) {
 *     // 최대 3개 프로세스가 동시에 실행
 *     42
 * }
 * ```
 *
 * @param lockName 락 이름
 * @param options 리더 선출 옵션
 * @param action 리더 그룹 슬롯 획득 시 수행할 작업
 * @return 작업 결과
 */
inline fun <T> RedissonClient.runIfLeaderGroup(
    lockName: String,
    options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default,
    crossinline action: () -> T,
): T {
    lockName.requireNotBlank("lockName")
    return RedissonLeaderGroupElection(this, options).runIfLeader(lockName) { action() }
}


/**
 * Redisson 분산 [RSemaphore]를 이용한 복수 리더 선출 구현체입니다.
 *
 * ## 동작
 * - `lockName`별로 Redis 분산 `RSemaphore(maxLeaders)`를 생성하여 동시 실행 수를 제한합니다.
 * - 슬롯이 가득 찬 경우 [LeaderGroupElectionOptions.waitTime] 내에 슬롯을 획득하지 못하면
 *   [RedisException]을 던집니다.
 * - 슬롯 획득 성공 시 `action`을 실행하고, 완료(또는 예외) 후 반드시 슬롯을 반납합니다.
 * - 여러 JVM 프로세스에 걸친 분산 동시 실행 제한에 적합합니다.
 *
 * ## [io.bluetape4k.leader.local.LocalLeaderGroupElection] 과의 차이
 * - [io.bluetape4k.leader.local.LocalLeaderGroupElection]은 단일 JVM 내 `java.util.concurrent.Semaphore`를 사용합니다.
 * - 이 구현체는 Redis 기반 `RSemaphore`를 사용하므로 여러 프로세스에서 동작합니다.
 *
 * ```kotlin
 * val options = RedissonLeaderGroupElectionOptions(maxLeaders = 3)
 * val election = RedissonLeaderGroupElection(redissonClient, options)
 *
 * // 최대 3개 스레드/프로세스가 동시에 실행
 * val result = election.runIfLeader("batch-job") { processChunk() }
 *
 * // 비동기 실행
 * val future = election.runAsyncIfLeader("batch-job") { CompletableFuture.completedFuture(42) }
 *
 * // 상태 조회
 * println(election.state("batch-job"))
 * ```
 *
 * @param redissonClient Redisson 클라이언트
 * @param maxLeaders 허용하는 최대 동시 리더 수. 기본값 2
 * @param options 리더 선출 옵션 (waitTime 사용)
 */
class RedissonLeaderGroupElection private constructor(
    private val redissonClient: RedissonClient,
    options: LeaderGroupElectionOptions,
): LeaderGroupElection {

    companion object: KLogging() {
        /**
         * [RedissonLeaderGroupElection] 인스턴스를 생성합니다.
         *
         * @param redissonClient Redisson 클라이언트
         * @param maxLeaders 허용하는 최대 동시 리더 수. 기본값 2
         * @param options 리더 선출 옵션
         */
        @JvmStatic
        operator fun invoke(
            redissonClient: RedissonClient,
            options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default,
        ): RedissonLeaderGroupElection {
            options.maxLeaders.requirePositiveNumber("maxLeaderse")
            return RedissonLeaderGroupElection(redissonClient, options)
        }
    }

    override val maxLeaders: Int = options.maxLeaders

    private val waitTime: Duration = options.waitTime

    /**
     * [lockName]에 해당하는 Redis [RSemaphore]를 가져오고, 허용 가능한 최대 슬롯 수를 [maxLeaders]로 초기화합니다.
     *
     * `trySetPermits`는 세마포어가 이미 초기화된 경우 아무 동작도 하지 않으므로 멱등적으로 호출할 수 있습니다.
     *
     * @param lockName 세마포어 키 이름
     * @return [maxLeaders] permits로 초기화된 [RSemaphore]
     */
    private fun getSemaphore(lockName: String): RSemaphore {
        lockName.requireNotBlank("lockName")
        val semaphore = redissonClient.getSemaphore(lockName)
        semaphore.trySetPermits(maxLeaders)
        return semaphore
    }

    /**
     * [lockName]에 대해 현재 활성(실행 중인) 리더 수를 반환합니다.
     *
     * `maxLeaders - availablePermits()`로 계산하므로 근사값입니다.
     */
    override fun activeCount(lockName: String): Int = maxLeaders - getSemaphore(lockName).availablePermits()

    /**
     * [lockName]에 대해 새 리더를 수용할 수 있는 남은 슬롯 수를 반환합니다.
     */
    override fun availableSlots(lockName: String): Int = getSemaphore(lockName).availablePermits()

    /**
     * [lockName]에 대한 현재 [LeaderGroupState] 스냅샷을 반환합니다.
     */
    override fun state(lockName: String): LeaderGroupState =
        LeaderGroupState(lockName, maxLeaders, activeCount(lockName))

    /**
     * [lockName]의 분산 [RSemaphore] 슬롯을 획득하고 [action]을 실행합니다.
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param action 슬롯 획득 성공 시 실행할 동기 작업
     * @return [action] 실행 결과
     * @throws RedisException 슬롯 획득 실패 또는 인터럽트 발생 시
     */
    override fun <T> runIfLeader(lockName: String, action: () -> T): T {
        lockName.requireNotBlank("lockName")

        val semaphore = getSemaphore(lockName)
        log.debug { "리더 그룹 슬롯 획득을 요청합니다. lockName=$lockName, maxLeaders=$maxLeaders" }

        val acquired = try {
            semaphore.tryAcquire(waitTime)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.error(e) { "슬롯 획득 대기 중 인터럽트가 발생했습니다. lockName=$lockName" }
            throw RedisException("Interrupted while acquiring semaphore slot. lockName=$lockName", e)
        }

        if (!acquired) {
            throw RedisException("Fail to acquire semaphore slot within waitTime. lockName=$lockName")
        }

        log.debug { "리더 그룹 슬롯을 획득하여 작업을 수행합니다. lockName=$lockName" }
        try {
            return action()
        } finally {
            semaphore.release()
            log.debug { "작업이 완료되어 슬롯을 반납했습니다. lockName=$lockName" }
        }
    }

    /**
     * [lockName]의 분산 [RSemaphore] 슬롯을 [executor]에서 획득하고 비동기 [action]을 실행합니다.
     *
     * - 슬롯이 가득 찬 경우 [waitTime] 내 슬롯을 획득하지 못하면 [RedisException]을 던집니다.
     * - [action] 예외 발생 시에도 슬롯은 반드시 반환됩니다.
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param executor 비동기 실행에 사용할 [Executor]
     * @param action 슬롯 획득 성공 시 실행할 비동기 작업
     * @return [action] 실행 결과를 담은 [CompletableFuture]
     * @throws RedisException 슬롯 획득 실패 또는 인터럽트 발생 시
     */
    override fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T> = CompletableFuture.supplyAsync(
        {
            lockName.requireNotBlank("lockName")
            val semaphore = getSemaphore(lockName)
            log.debug { "리더 그룹 슬롯 획득을 요청합니다 (비동기). lockName=$lockName, maxLeaders=$maxLeaders" }

            val acquired = try {
                semaphore.tryAcquire(waitTime)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.error(e) { "슬롯 획득 대기 중 인터럽트가 발생했습니다. lockName=$lockName" }
                throw RedisException("Interrupted while acquiring semaphore slot. lockName=$lockName", e)
            }

            if (!acquired) {
                throw RedisException("Fail to acquire semaphore slot within waitTime. lockName=$lockName")
            }

            log.debug { "리더 그룹 슬롯을 획득하여 비동기 작업을 수행합니다. lockName=$lockName" }
            try {
                action().join()
            } finally {
                semaphore.release()
                log.debug { "비동기 작업이 완료되어 슬롯을 반납했습니다. lockName=$lockName" }
            }
        }, executor
    )
}
