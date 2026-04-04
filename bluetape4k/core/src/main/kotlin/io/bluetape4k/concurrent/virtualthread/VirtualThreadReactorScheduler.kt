package io.bluetape4k.concurrent.virtualthread

import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ExecutorService

/**
 * Reactor에서 Virtual Thread를 사용하기 위한 공유 [Scheduler]입니다.
 *
 * 내부적으로 단일 [ExecutorService] 인스턴스를 재사용합니다.
 * 여러 Mono/Flux 체인에서 공유 스케줄러가 필요할 때 적합합니다.
 *
 * ```kotlin
 * Mono.fromCallable {
 *         // Virtual Thread 위에서 실행
 *         Thread.currentThread().isVirtual  // true
 *         "result"
 *     }
 *     .subscribeOn(Schedulers.virtualThread)
 *     .block()
 *     .let { println(it) } // "result"
 * ```
 */
@Suppress("UnusedReceiverParameter")
val Schedulers.virtualThread: Scheduler
    get() = Schedulers.fromExecutorService(VirtualThreadExecutor)

/**
 * Reactor에서 Virtual Thread를 사용하기 위한 새 [Scheduler]를 생성합니다.
 *
 * 매 호출마다 새로운 Virtual Thread 기반 [ExecutorService]를 생성하여 [Scheduler]로 반환합니다.
 * 독립적인 스케줄러 인스턴스가 필요할 때 사용하세요. 재사용하려면 반환값을 변수에 저장하세요.
 *
 * ```kotlin
 * val scheduler = Schedulers.newVirtualThread()
 *
 * Flux.range(1, 5)
 *     .flatMap { n ->
 *         Mono.fromCallable {
 *             // Virtual Thread 위에서 실행
 *             Thread.currentThread().isVirtual  // true
 *             n * 2
 *         }.subscribeOn(scheduler)
 *     }
 *     .collectList()
 *     .block()
 *     .let { println(it) } // [2, 4, 6, 8, 10]
 * ```
 *
 * 매 호출마다 새로운 Virtual Thread 기반 Scheduler를 생성합니다. 재사용하려면 변수에 저장하세요.
 */
@Suppress("UnusedReceiverParameter")
fun Schedulers.newVirtualThread(): Scheduler =
    Schedulers.fromExecutorService(VirtualThreads.executorService())
