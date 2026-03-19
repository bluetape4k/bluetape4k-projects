package io.bluetape4k.aws.kotlin.http

import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngine

/**
 * AWS SDK for Kotlin 클라이언트에서 재사용할 HTTP 엔진 singleton 제공자입니다.
 *
 * 기본 엔진은 [Crt]를 사용하며, 필요 시 [OkHttp] 엔진을 선택적으로 사용할 수 있습니다.
 */
object HttpClientEngineProvider {

    /**
     * AWS CRT 기반 HTTP 엔진 singleton 공급자입니다.
     *
     * `crtHttpEngineOf()`에서 이미 `ShutdownQueue` 등록을 수행하므로,
     * 여기서는 추가 등록을 하지 않습니다.
     */
    object Crt {
        @JvmStatic
        val httpEngine: CrtHttpEngine by lazy { crtHttpEngineOf() }
    }

    /**
     * OkHttp 기반 HTTP 엔진 singleton 공급자입니다.
     *
     * `okHttpEngineOf()`에서 이미 `ShutdownQueue` 등록을 수행하므로,
     * 여기서는 추가 등록을 하지 않습니다.
     */
    object OkHttp {
        @JvmStatic
        val httpEngine: OkHttpEngine by lazy { okHttpEngineOf() }
    }

    /**
     * 기본 HTTP 엔진입니다.
     *
     * 현재 기본값은 CRT singleton 엔진([Crt.httpEngine])이며,
     * 모듈 전반에서 동일 인스턴스를 재사용합니다.
     */
    val defaultHttpEngine get() = Crt.httpEngine
}
