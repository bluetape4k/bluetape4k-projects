package io.bluetape4k.images.vips.java21.internal

import com.criteo.vips.VipsContext

/**
 * JVips 네이티브 런타임 어댑터 인터페이스.
 *
 * 테스트에서 실제 JNI 호출을 대체할 수 있는 seam 역할을 합니다.
 * 프로덕션 구현체는 [DefaultJVipsNativeRuntime]입니다.
 */
internal interface JVipsNativeRuntime {
    fun nativeInit(concurrency: Int)
    fun nativeShutdown()
}

/**
 * JVips JNI 바인딩을 직접 호출하는 기본 구현체.
 *
 * `VipsContext`를 최초로 참조하는 순간 `Vips` 클래스 로더가 `Vips.init()`을 정적 초기화에서 호출합니다.
 * 따라서 별도로 `Vips.init()`을 호출할 필요가 없습니다.
 */
internal object DefaultJVipsNativeRuntime : JVipsNativeRuntime {
    override fun nativeInit(concurrency: Int) {
        // VipsContext 참조가 Vips 정적 초기화를 트리거하여 libvips를 로드합니다.
        VipsContext.setConcurrency(concurrency)
    }

    override fun nativeShutdown() {
        VipsContext.shutdown()
    }
}
