package io.bluetape4k.aws.http

import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.http.async.SdkAsyncHttpClient

/**
 * 비동기 [SdkAsyncHttpClient] 공용 인스턴스를 지연 생성해 제공하는 Provider입니다.
 *
 * ## 동작/계약
 * - 하위 객체별 `httpClient`는 `by lazy`로 최초 접근 시 1회 생성된다.
 * - 생성된 클라이언트는 [ShutdownQueue]에 등록되어 종료 시 정리된다.
 *
 * ```kotlin
 * val client = SdkAsyncHttpClientProvider.defaultHttpClient
 * // client === SdkAsyncHttpClientProvider.Netty.httpClient
 * ```
 *
 * 참고: [AWS HTTP 클라이언트](https://docs.aws.amazon.com/ko_kr/sdk-for-java/latest/developer-guide/http-configuration.html)
 */
object SdkAsyncHttpClientProvider {

    /**
     * Netty NIO 기반 비동기 HTTP 클라이언트를 제공합니다.
     *
     * ## 동작/계약
     * - [nettyNioAsyncHttpClientOf] 기본 설정으로 클라이언트를 생성한다.
     * - 생성 직후 [ShutdownQueue]에 등록한 인스턴스를 재사용한다.
     *
     * ```kotlin
     * val netty = SdkAsyncHttpClientProvider.Netty.httpClient
     * // netty === SdkAsyncHttpClientProvider.Netty.httpClient
     * ```
     */
    object Netty {
        /**
         * Netty 기반 공용 [SdkAsyncHttpClient] 인스턴스입니다.
         *
         * ## 동작/계약
         * - 최초 접근 시 1회 생성되고 이후 동일 인스턴스를 반환한다.
         * - 생성된 인스턴스는 종료 큐에 등록된다.
         *
         * ```kotlin
         * val first = SdkAsyncHttpClientProvider.Netty.httpClient
         * val second = SdkAsyncHttpClientProvider.Netty.httpClient
         * // first === second
         * ```
         */
        @JvmStatic
        val httpClient: SdkAsyncHttpClient by lazy {
            nettyNioAsyncHttpClientOf().apply {
                ShutdownQueue.register(this)
            }
        }
    }

    /**
     * AWS CRT 기반 비동기 HTTP 클라이언트를 제공합니다.
     *
     * ## 동작/계약
     * - [awsCrtAsyncHttpClientOf] 기본 설정으로 클라이언트를 생성한다.
     * - 생성 직후 [ShutdownQueue]에 등록한 인스턴스를 재사용한다.
     *
     * ```kotlin
     * val crt = SdkAsyncHttpClientProvider.AwsCrt.httpClient
     * // crt === SdkAsyncHttpClientProvider.AwsCrt.httpClient
     * ```
     */
    object AwsCrt {
        /**
         * AWS CRT 기반 공용 [SdkAsyncHttpClient] 인스턴스입니다.
         *
         * ## 동작/계약
         * - 최초 접근 시 1회 생성되고 이후 동일 인스턴스를 반환한다.
         * - 생성된 인스턴스는 종료 큐에 등록된다.
         *
         * ```kotlin
         * val first = SdkAsyncHttpClientProvider.AwsCrt.httpClient
         * val second = SdkAsyncHttpClientProvider.AwsCrt.httpClient
         * // first === second
         * ```
         */
        @JvmStatic
        val httpClient: SdkAsyncHttpClient by lazy {
            awsCrtAsyncHttpClientOf().apply {
                ShutdownQueue.register(this)
            }
        }
    }

    /**
     * 기본 비동기 HTTP 클라이언트로 Netty 구현을 반환합니다.
     *
     * ## 동작/계약
     * - [Netty.httpClient] 참조를 그대로 반환한다.
     * - 호출할 때마다 새 인스턴스를 생성하지 않는다.
     *
     * ```kotlin
     * val defaultClient = SdkAsyncHttpClientProvider.defaultHttpClient
     * // defaultClient === SdkAsyncHttpClientProvider.Netty.httpClient
     * ```
     */
    val defaultHttpClient: SdkAsyncHttpClient get() = Netty.httpClient
}
