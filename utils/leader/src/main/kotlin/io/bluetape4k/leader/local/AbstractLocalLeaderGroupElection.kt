package io.bluetape4k.leader.local

import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.leader.LeaderGroupElectionState
import io.bluetape4k.leader.LeaderGroupState
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requirePositiveNumber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

/**
 * 로컬(단일 JVM) 리더 그룹 선출 구현체들의 공통 상태 관리를 제공하는 추상 클래스입니다.
 *
 * ## 역할
 * - `lockName`별 [Semaphore] 풀을 [ConcurrentHashMap]으로 관리합니다.
 * - [LeaderGroupElectionState]의 상태 조회 메서드([activeCount], [availableSlots], [state])를 구현합니다.
 * - 하위 클래스는 [getSemaphore]를 이용해 슬롯을 획득/반환하고 실행 로직을 구현합니다.
 *
 * ## 하위 클래스
 * - [LocalLeaderGroupElection]: 동기 + 비동기([java.util.concurrent.CompletableFuture]) 실행
 * - [LocalAsyncLeaderGroupElection]: 비동기([java.util.concurrent.CompletableFuture]) 실행만
 * - [LocalVirtualThreadLeaderGroupElection]: [io.bluetape4k.concurrent.virtualthread.VirtualFuture] 실행
 *
 * @param maxLeaders 허용하는 최대 동시 리더 수
 */
abstract class AbstractLocalLeaderGroupElection(
    options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default,
): LeaderGroupElectionState {

    init {
        options.maxLeaders.requirePositiveNumber("maxLeaders")
    }

    override val maxLeaders: Int = options.maxLeaders

    private val semaphores = ConcurrentHashMap<String, Semaphore>()

    /**
     * [lockName]에 대한 [Semaphore]를 반환합니다. 없으면 `Semaphore(maxLeaders, fair=true)`를 생성합니다.
     */
    protected fun getSemaphore(lockName: String): Semaphore {
        lockName.requireNotBlank("lockName")
        return semaphores.computeIfAbsent(lockName) { Semaphore(maxLeaders, true) }
    }

    /**
     * [lockName]의 슬롯을 획득한 상태에서 [action]을 실행하고, 완료 시 슬롯을 반환합니다.
     */
    protected inline fun <T> withPermit(lockName: String, action: () -> T): T {
        val semaphore = getSemaphore(lockName)
        semaphore.acquire()
        try {
            return action()
        } finally {
            semaphore.release()
        }
    }

    /**
     * [lockName]에 대해 현재 활성(실행 중인) 리더 수를 반환합니다.
     */
    override fun activeCount(lockName: String): Int =
        maxLeaders - getSemaphore(lockName).availablePermits()

    /**
     * [lockName]에 대해 새 리더를 수용할 수 있는 남은 슬롯 수를 반환합니다.
     */
    override fun availableSlots(lockName: String): Int =
        getSemaphore(lockName).availablePermits()

    /**
     * [lockName]에 대한 현재 [LeaderGroupState] 스냅샷을 반환합니다.
     */
    override fun state(lockName: String): LeaderGroupState =
        LeaderGroupState(lockName, maxLeaders, activeCount(lockName))
}
