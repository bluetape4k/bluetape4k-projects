package io.bluetape4k.leader.local

import io.bluetape4k.leader.LeaderElectionOptions
import io.bluetape4k.support.requireNotBlank
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * 로컬(단일 JVM) 리더 선출 구현체들의 공통 락 관리를 제공하는 추상 클래스입니다.
 *
 * ## 역할
 * - `lockName`별 [ReentrantLock] 풀을 [ConcurrentHashMap]으로 관리합니다.
 * - 하위 클래스는 [getLock]을 이용해 락을 획득/반환하고 실행 로직을 구현합니다.
 *
 * ## 하위 클래스
 * - [LocalLeaderElection]: 동기 + 비동기([java.util.concurrent.CompletableFuture]) 실행
 * - [LocalAsyncLeaderElection]: 비동기([java.util.concurrent.CompletableFuture]) 실행만
 * - [LocalVirtualThreadLeaderElection]: [io.bluetape4k.concurrent.virtualthread.VirtualFuture] 실행
 */
abstract class AbstractLocalLeaderElection(
    protected val options: LeaderElectionOptions = LeaderElectionOptions.Default,
) {

    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    /**
     * [lockName]에 대한 [ReentrantLock]을 반환합니다. 없으면 새로 생성합니다.
     */
    protected fun getLock(lockName: String): ReentrantLock {
        lockName.requireNotBlank("lockName")
        return locks.computeIfAbsent(lockName) { ReentrantLock() }
    }
}
