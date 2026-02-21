package io.bluetape4k.concurrent.virtualthread

import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory

/**
 * JDK별 Virtual Thread 구현체를 추상화한 인터페이스입니다.
 */
interface VirtualThreadRuntime {

    /**
     * 구현체 식별 이름입니다.
     */
    val runtimeName: String

    /**
     * 구현체 우선순위입니다. 값이 클수록 우선합니다.
     */
    val priority: Int

    /**
     * 현재 런타임에서 사용 가능한 구현체인지 여부를 반환합니다.
     */
    fun isSupported(): Boolean

    /**
     * Virtual Thread 전용 [ThreadFactory]를 생성합니다.
     */
    fun threadFactory(prefix: String = "vt-"): ThreadFactory

    /**
     * Task-per-virtual-thread [ExecutorService]를 생성합니다.
     */
    fun executorService(): ExecutorService
}

