package io.bluetape4k.images.vips.java21.internal

import io.bluetape4k.logging.KLogging
import com.criteo.vips.VipsImage
import java.lang.ref.Cleaner

/**
 * JVips [VipsImage] 네이티브 핸들 래퍼.
 *
 * 참조 카운트(ref count) 없이 단일 소유권 모델을 사용합니다.
 * [Cleaner]를 통해 GC 시 누수 감지 경고를 기록합니다.
 *
 * **Cleaner 안전성**: 클리너 람다는 반드시 `NativeHandle` 인스턴스(`this`)를 캡처하지 않아야 합니다.
 * 강한 참조가 람다에 포함되면 GC가 객체를 수집할 수 없어 클리너가 영영 실행되지 않습니다.
 * 이 구현은 `vipsImage` 참조만 캡처하므로 안전합니다.
 */
internal class NativeHandle(val vipsImage: VipsImage) : AutoCloseable {

    companion object : KLogging() {
        // Cleaner는 반드시 companion object(정적)에 있어야 합니다.
        // per-instance Cleaner.create()는 매번 새 데몬 스레드를 생성합니다.
        private val CLEANER: Cleaner = Cleaner.create()
    }

    private val cleanable: Cleaner.Cleanable

    init {
        // `this`를 캡처하지 않도록 로컬 변수에만 복사합니다.
        val capturedImage = vipsImage
        // companion log를 로컬로 캡처합니다 (Cleaner 람다에서 companion 프로퍼티 직접 참조 방지).
        val capturedLog = log
        cleanable = CLEANER.register(this) {
            // 이 람다는 NativeHandle 인스턴스를 강하게 참조하지 않습니다.
            try {
                capturedImage.release()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                capturedLog.warn("NativeHandle was released via GC cleaner — close() was not called explicitly.", e)
            }
        }
    }

    /**
     * 네이티브 리소스를 명시적으로 해제합니다.
     *
     * 이 메서드를 호출하면 클리너 등록이 취소되고 [VipsImage.release]가 즉시 호출됩니다.
     * `use {}` 블록 또는 명시적 `close()` 호출로 반드시 해제하십시오.
     */
    override fun close() {
        cleanable.clean()
    }
}
