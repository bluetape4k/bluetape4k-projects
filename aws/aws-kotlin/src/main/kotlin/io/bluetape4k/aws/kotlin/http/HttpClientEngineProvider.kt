package io.bluetape4k.aws.kotlin.http

import aws.smithy.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.smithy.kotlin.runtime.http.engine.okhttp.OkHttpEngine

/**
 * AWS SDK for Kotlin 클라이언트에서 재사용할 HTTP 엔진 singleton 제공자입니다.
 *
 * ## 주의
 * 여기서 제공하는 엔진들은 여러 클라이언트가 공유하는 singleton입니다.
 * 이 엔진을 클라이언트 생성 시 명시적으로 전달하면 SDK는 `isManaged=false`로 처리하여
 * `client.close()` 시 엔진을 종료하지 않습니다.
 *
 * 단일 클라이언트 사용 패턴에서는 `httpClient` 파라미터를 생략해 SDK가 엔진을 직접 관리하도록 하거나,
 * `withXxxClient { }` 패턴을 사용하세요.
 *
 * 여러 클라이언트가 동일 엔진을 공유해야 하는 경우에만 이 singleton을 명시적으로 전달하고,
 * 애플리케이션 종료 시점에 직접 `close()`를 호출하세요.
 */
object HttpClientEngineProvider {

    /**
     * AWS CRT 기반 HTTP 엔진 singleton 공급자입니다.
     *
     * CRT 엔진은 non-daemon 스레드를 사용하므로, 이 엔진을 공유하는 모든 클라이언트를
     * 닫은 후에도 JVM이 종료되려면 이 엔진을 명시적으로 `close()`해야 합니다.
     */
    object Crt {
        @JvmStatic
        val httpEngine: CrtHttpEngine by lazy { crtHttpEngineOf() }
    }

    /**
     * OkHttp 기반 HTTP 엔진 singleton 공급자입니다.
     */
    object OkHttp {
        @JvmStatic
        val httpEngine: OkHttpEngine by lazy { okHttpEngineOf() }
    }

    /**
     * 기본 HTTP 엔진입니다. 현재 기본값은 CRT singleton 엔진([Crt.httpEngine])입니다.
     */
    val defaultHttpEngine get() = Crt.httpEngine
}
