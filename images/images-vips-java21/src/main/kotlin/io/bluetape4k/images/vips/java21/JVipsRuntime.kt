package io.bluetape4k.images.vips.java21

import io.bluetape4k.images.vips.VipsInitializationException
import io.bluetape4k.images.vips.VipsLimits
import io.bluetape4k.images.vips.VipsRuntime
import io.bluetape4k.images.vips.java21.internal.DefaultJVipsNativeRuntime
import io.bluetape4k.images.vips.java21.internal.JVipsNativeRuntime
import io.bluetape4k.logging.KLogging
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.atomic.AtomicReference

/**
 * JVips(JNI) 기반 libvips 런타임 싱글턴.
 *
 * **종료 계약(Terminal Contract)**: [shutdown] 이후 [init]을 호출하면 [VipsInitializationException]이 발생합니다.
 * libvips는 `vips_shutdown()` 이후 `VIPS_INIT()` 재호출을 지원하지 않으므로, 프로세스를 재시작해야 합니다.
 *
 * **스레드 안전성**: `AtomicReference<RuntimeState>` CAS로 스레드 안전성을 보장합니다.
 * `@Synchronized`를 사용하지 않습니다 (Virtual Thread 핀닝 방지).
 *
 * **Spring devtools 경고**: [shutdown]을 `@PreDestroy` 빈 메서드로 등록하지 마십시오.
 * devtools가 `ApplicationContext`를 재시작할 때 [shutdown] → [init] 순으로 호출되어
 * `VipsInitializationException`이 발생합니다. `Runtime.addShutdownHook`만 사용하십시오.
 */
object JVipsRuntime : VipsRuntime, KLogging() {

    private enum class RuntimeState { UNINITIALIZED, INITIALIZING, INITIALIZED, SHUTDOWN }

    private val state = AtomicReference(RuntimeState.UNINITIALIZED)

    @VisibleForTesting
    internal var nativeRuntime: JVipsNativeRuntime = DefaultJVipsNativeRuntime

    /** maxPixels 설정값 (init 시 저장) */
    @Volatile
    private var _maxPixels: Long = VipsLimits.DEFAULT_MAX_PIXELS

    /** 허용할 최대 픽셀 수 `width × height × bands` */
    val maxPixels: Long get() = _maxPixels

    override fun init(concurrency: Int, maxPixels: Long) {
        // Fast path
        when (state.get()) {
            RuntimeState.INITIALIZED -> return
            RuntimeState.SHUTDOWN -> throw VipsInitializationException(
                "libvips has been shut down — restart the process to re-initialize"
            )
            else -> {}
        }

        if (!state.compareAndSet(RuntimeState.UNINITIALIZED, RuntimeState.INITIALIZING)) {
            // 다른 스레드가 CAS에서 이겼습니다. 초기화가 완료될 때까지 스핀 대기합니다.
            // ⚠️ 여기서 그냥 return 하면 호출자가 초기화 완료 전에 진행하게 됩니다.
            var spinCount = 0
            while (state.get() == RuntimeState.INITIALIZING) {
                if (++spinCount > 10_000) {
                    java.util.concurrent.locks.LockSupport.parkNanos(1_000_000L) // 1ms backoff
                    spinCount = 0
                } else {
                    Thread.onSpinWait()
                }
            }
            when (state.get()) {
                RuntimeState.INITIALIZED -> return
                RuntimeState.SHUTDOWN -> throw VipsInitializationException(
                    "libvips was shut down during concurrent initialization"
                )
                RuntimeState.UNINITIALIZED -> throw VipsInitializationException(
                    "Concurrent initialization attempt failed — retry"
                )
                else -> {} // 도달 불가: INITIALIZING 루프를 빠져나왔으므로
            }
            return
        }

        // 이 스레드가 INITIALIZING 슬롯을 소유합니다.
        try {
            nativeRuntime.nativeInit(concurrency)
            _maxPixels = maxPixels
            state.set(RuntimeState.INITIALIZED)
            log.debug("JVipsRuntime initialized: concurrency=$concurrency, maxPixels=$maxPixels")
        } catch (e: Error) {
            // UnsatisfiedLinkError, NoClassDefFoundError 등 — 상태 복구 후 원본 Error 재던짐
            state.set(RuntimeState.UNINITIALIZED)
            throw e
        } catch (e: Exception) {
            state.set(RuntimeState.UNINITIALIZED)  // 재시도 허용
            throw VipsInitializationException("libvips initialization failed", e)
        }
    }

    override fun shutdown() {
        // INITIALIZING 중 shutdown()이 호출되면 spin-wait 후 전이.
        // UNINITIALIZED/SHUTDOWN 상태에서는 아무것도 하지 않음.
        while (true) {
            when (state.get()) {
                RuntimeState.SHUTDOWN, RuntimeState.UNINITIALIZED -> return
                RuntimeState.INITIALIZED -> {
                    if (state.compareAndSet(RuntimeState.INITIALIZED, RuntimeState.SHUTDOWN)) {
                        nativeRuntime.nativeShutdown()
                        log.debug("JVipsRuntime shut down")
                        return
                    }
                }
                RuntimeState.INITIALIZING -> Thread.onSpinWait()
            }
        }
    }

    override val isInitialized: Boolean
        get() = state.get() == RuntimeState.INITIALIZED

    override val isShutdown: Boolean
        get() = state.get() == RuntimeState.SHUTDOWN

    /**
     * 테스트 전용: 상태를 UNINITIALIZED로 리셋하고 기본 nativeRuntime을 복원합니다.
     *
     * `@AfterEach`에서 호출하여 테스트 간 상태 누수를 방지합니다.
     * 프로덕션 코드에서 호출하지 마십시오.
     */
    @VisibleForTesting
    internal fun resetForTest() {
        state.set(RuntimeState.UNINITIALIZED)
        nativeRuntime = DefaultJVipsNativeRuntime
        _maxPixels = VipsLimits.DEFAULT_MAX_PIXELS
    }
}
