package io.bluetape4k.images.vips

/**
 * libvips 런타임 라이프사이클 관리 인터페이스.
 *
 * **종료 계약(Terminal Contract)**: [shutdown]은 되돌릴 수 없는 연산입니다.
 * `vips_shutdown()` 이후에는 `VIPS_INIT()`을 재호출할 수 없습니다.
 * [isShutdown]이 `true`인 상태에서 [init]을 호출하면 [VipsInitializationException]이 발생합니다.
 * 이 경우 프로세스를 재시작해야 합니다.
 *
 * **스레드 안전성**: 구현체는 `AtomicReference<State>` 기반 CAS로 스레드 안전성을 보장합니다.
 * `@Synchronized`를 사용하지 않습니다 (Virtual Thread 핀닝 문제 방지).
 *
 * **Spring devtools 경고**: [shutdown]을 `@PreDestroy` 빈 메서드로 등록하지 마십시오.
 * devtools가 `ApplicationContext`를 재시작하면 [shutdown] → [init] 순서로 호출되어
 * `VipsInitializationException`이 발생합니다. JVM 종료 훅(`Runtime.addShutdownHook`)만 사용하십시오.
 */
interface VipsRuntime {

    /**
     * libvips 런타임을 초기화합니다.
     *
     * 이미 초기화된 경우 즉시 반환합니다.
     * 다른 스레드가 초기화 중인 경우 완료될 때까지 스핀 대기합니다.
     *
     * @param concurrency libvips 내부 스레드 수 (기본값: 4)
     * @param maxPixels 허용할 최대 픽셀 수 `width × height × bands` (기본값: 1억 5천만)
     * @throws VipsInitializationException 초기화 실패 또는 [shutdown] 이후 재호출 시
     */
    fun init(concurrency: Int = 4, maxPixels: Long = VipsLimits.DEFAULT_MAX_PIXELS)

    /**
     * libvips 런타임을 종료합니다.
     *
     * 이미 종료된 경우 아무 동작도 하지 않습니다.
     * **되돌릴 수 없음**: 이후 [init] 호출은 [VipsInitializationException]을 발생시킵니다.
     */
    fun shutdown()

    /** 런타임이 성공적으로 초기화된 상태이면 `true` */
    val isInitialized: Boolean

    /** [shutdown] 이후 `true`. 이 상태에서 [init]을 호출하면 예외가 발생합니다. */
    val isShutdown: Boolean
}
