package io.bluetape4k.concurrent.virtualthread

import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ExecutorService

/**
 * Reactor에서 Virtual Thread를 사용하기 위한 [Scheduler]
 * Virtual thread를 사용하는 [ExecutorService]를 반환
 */
@Suppress("UnusedReceiverParameter")
val Schedulers.virtualThread: Scheduler
    get() = Schedulers.fromExecutorService(VirtualThreadExecutor)

/**
 * Reactor에서 Virtual Thread를 사용하기 위한 [Scheduler]
 * Virtual thread를 사용하는 새로운 [ExecutorService] 를 생성하여 반환합니다.
 *
 * 매 호출마다 새로운 Virtual Thread 기반 Scheduler를 생성합니다. 재사용하려면 변수에 저장하세요.
 */
@Suppress("UnusedReceiverParameter")
fun Schedulers.newVirtualThread(): Scheduler =
    Schedulers.fromExecutorService(VirtualThreads.executorService())
