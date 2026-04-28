package io.bluetape4k.images.vips.java25.internal

import app.photofox.vipsffm.Vips

/**
 * vips-ffm 네이티브 런타임 어댑터 인터페이스.
 *
 * 테스트에서 실제 FFM 호출을 대체할 수 있는 seam 역할을 합니다.
 */
internal interface FfmVipsNativeRuntime {
    fun nativeInit()
    fun nativeShutdown()
}

/**
 * vips-ffm FFM 바인딩을 직접 호출하는 기본 구현체.
 */
internal object DefaultFfmVipsNativeRuntime : FfmVipsNativeRuntime {
    override fun nativeInit() {
        Vips.init()
    }

    override fun nativeShutdown() {
        Vips.shutdown()
    }
}
