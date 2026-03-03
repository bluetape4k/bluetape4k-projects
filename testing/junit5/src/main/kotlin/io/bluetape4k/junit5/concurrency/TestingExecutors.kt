package io.bluetape4k.junit5.concurrency

import io.bluetape4k.junit5.concurrency.TestingExecutors.newVirtualThreadFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory


/**
 * 테스트에서 자주 쓰는 [ExecutorService]와 [ThreadFactory] 생성기를 제공합니다.
 *
 * ## 동작/계약
 * - 각 함수는 JDK `Executors` 팩토리를 래핑해 새 인스턴스를 반환합니다.
 * - 호출 시마다 새 실행자를 할당하며 캐시하지 않습니다.
 * - 전달 값 검증은 JDK 구현의 규칙을 그대로 따르며 잘못된 값은 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val fixed = TestingExecutors.newFixedThreadPool(2)
 * val vt = TestingExecutors.newVirtualThreadPerTaskExecutor("spec-vt-")
 * // fixed.isShutdown == false && vt.isShutdown == false
 * ```
 */
object TestingExecutors {

    /** 가상 스레드 이름 접두사의 기본값입니다. */
    const val DEFAULT_VIRTUAL_THREAD_NAME = "bluetape4k-test-vt-"

    /**
     * 고정 크기 스레드 풀 실행자를 생성합니다.
     *
     * ## 동작/계약
     * - 호출마다 독립적인 [ExecutorService]를 새로 만듭니다.
     * - 실행자 종료는 호출자 책임입니다.
     *
     * ```kotlin
     * val executor = TestingExecutors.newFixedThreadPool(4)
     * // executor is ExecutorService
     * ```
     */
    fun newFixedThreadPool(nThreads: Int): ExecutorService {
        return Executors.newFixedThreadPool(nThreads)
    }

    /**
     * 스케줄 가능한 실행자를 생성합니다.
     *
     * ## 동작/계약
     * - `corePoolSize`를 사용해 스케줄드 스레드 풀을 초기화합니다.
     * - 실행자 lifecycle(`shutdown`)은 호출자가 관리해야 합니다.
     *
     * ```kotlin
     * val scheduler = TestingExecutors.newScheduledExecutorService(1)
     * // scheduler is ScheduledExecutorService
     * ```
     */
    fun newScheduledExecutorService(corePoolSize: Int = 0): ScheduledExecutorService {
        return Executors.newScheduledThreadPool(corePoolSize)
    }

    /**
     * 가상 스레드 per-task 실행자를 생성합니다.
     *
     * ## 동작/계약
     * - 내부적으로 [newVirtualThreadFactory]로 팩토리를 만든 뒤 per-task executor를 생성합니다.
     * - `prefix`는 스레드 이름에 반영되며 `bluetape4k-test-vt-`가 기본값입니다.
     *
     * ```kotlin
     * val executor = TestingExecutors.newVirtualThreadPerTaskExecutor("junit-vt-")
     * // submit한 작업은 virtual thread에서 실행
     * ```
     *
     * @param prefix 생성할 가상 스레드 이름 접두사
     */
    fun newVirtualThreadPerTaskExecutor(prefix: String = DEFAULT_VIRTUAL_THREAD_NAME): ExecutorService {
        return Executors.newThreadPerTaskExecutor(newVirtualThreadFactory(prefix))
    }

    /**
     * 가상 스레드 [ThreadFactory]를 생성합니다.
     *
     * ## 동작/계약
     * - 시작 인덱스 0 기반 이름 정책(`prefix + index`)을 사용합니다.
     * - 팩토리 자체는 상태를 저장하지 않는 경량 객체입니다.
     *
     * ```kotlin
     * val factory = TestingExecutors.newVirtualThreadFactory("vt-")
     * val thread = factory.newThread { }
     * // thread.name.startsWith("vt-") == true
     * ```
     */
    fun newVirtualThreadFactory(prefix: String = DEFAULT_VIRTUAL_THREAD_NAME): ThreadFactory {
        return Thread.ofVirtual().name(prefix, 0).factory()
    }
}
