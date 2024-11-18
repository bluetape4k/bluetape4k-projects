package io.bluetape4k.junit5.concurrency

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory


/**
 * Executors 를 제공하는 유틸리티 클래스입니다.
 *
 * ```
 * val executor = TestingExecutors.newFixedThreadPool(4)
 * val virtualExecutor = TestingExecutors.newVirtualThreadPerTaskExecutor()
 * ```
 */
object TestingExecutors {

    const val DEFAULT_VIRTUAL_THREAD_NAME = "bluetape4k-test-vt-"

    /**
     * 고정 크기의 쏘레드 풀을 가진 [ExecutorService]를 생성합니다.
     */
    fun newFixedThreadPool(nThreads: Int): ExecutorService {
        return Executors.newFixedThreadPool(nThreads)
    }

    /**
     * 스케쥴이 가능한 [ScheduledExecutorService]를 생성합니다.
     */
    fun newScheduledExecutorService(corePoolSize: Int = 0): ScheduledExecutorService {
        return Executors.newScheduledThreadPool(corePoolSize)
    }

    /**
     * 가상 쓰레드를 사용하는 [ExecutorService]를 생성합니다.
     *
     * @param prefix 쓰레드 이름의 접두사
     */
    fun newVirtualThreadPerTaskExecutor(prefix: String = DEFAULT_VIRTUAL_THREAD_NAME): ExecutorService {
        return Executors.newThreadPerTaskExecutor(newVirtualThreadFactory(prefix))
    }

    /**
     * 가상 쓰레드 팩토리([ThreadFactory])를 생성합니다.
     */
    fun newVirtualThreadFactory(prefix: String = DEFAULT_VIRTUAL_THREAD_NAME): ThreadFactory {
        return Thread.ofVirtual().name(prefix, 0).factory()
    }
}
