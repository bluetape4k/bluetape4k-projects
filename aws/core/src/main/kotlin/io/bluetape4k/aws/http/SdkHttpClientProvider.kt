package io.bluetape4k.aws.http

import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient

/**
 * 동기 [SdkHttpClient] 공용 인스턴스를 지연 생성해 제공하는 Provider입니다.
 *
 * ## 동작/계약
 * - 하위 객체별 `httpClient`는 `by lazy`로 최초 접근 시 1회 생성된다.
 * - 생성된 클라이언트는 [ShutdownQueue]에 등록되어 종료 훅에서 정리된다.
 *
 * ```kotlin
 * val client = SdkHttpClientProvider.defaultHttpClient
 * // client == SdkHttpClientProvider.Apache.httpClient
 * ```
 *
 * 참고: [AWS HTTP 클라이언트](https://docs.aws.amazon.com/ko_kr/sdk-for-java/latest/developer-guide/http-configuration.html)
 */
object SdkHttpClientProvider {

    /**
     * Apache 기반 동기 HTTP 클라이언트를 제공합니다.
     *
     * ## 동작/계약
     * - [ApacheHttpClient] 빌더 기본값으로 클라이언트를 생성한다.
     * - 생성 직후 [ShutdownQueue]에 등록한 인스턴스를 캐시해 재사용한다.
     *
     * ```kotlin
     * val apache = SdkHttpClientProvider.Apache.httpClient
     * // apache === SdkHttpClientProvider.Apache.httpClient
     * ```
     */
    object Apache {

        /**
         * Apache 기반 공용 [SdkHttpClient] 인스턴스입니다.
         *
         * ## 동작/계약
         * - 최초 접근 시 1회 생성되고 이후 동일 인스턴스를 반환한다.
         * - 생성된 인스턴스는 종료 큐에 등록된다.
         *
         * ```kotlin
         * val first = SdkHttpClientProvider.Apache.httpClient
         * val second = SdkHttpClientProvider.Apache.httpClient
         * // first === second
         * ```
         */
        val httpClient: SdkHttpClient by lazy {
            ApacheHttpClient.builder().build()
                .apply {
                    ShutdownQueue.register(this)
                }
        }
    }

    /**
     * URLConnection 기반 동기 HTTP 클라이언트를 제공합니다.
     *
     * ## 동작/계약
     * - [UrlConnectionHttpClient] 기본 빌더로 클라이언트를 생성한다.
     * - 생성 직후 [ShutdownQueue]에 등록한 인스턴스를 재사용한다.
     *
     * ```kotlin
     * val urlConnection = SdkHttpClientProvider.UrlConnection.httpClient
     * // urlConnection === SdkHttpClientProvider.UrlConnection.httpClient
     * ```
     */
    object UrlConnection {

        /**
         * URLConnection 기반 공용 [SdkHttpClient] 인스턴스입니다.
         *
         * ## 동작/계약
         * - 최초 접근 시 1회 생성되고 이후 동일 인스턴스를 반환한다.
         * - 생성된 인스턴스는 종료 큐에 등록된다.
         *
         * ```kotlin
         * val first = SdkHttpClientProvider.UrlConnection.httpClient
         * val second = SdkHttpClientProvider.UrlConnection.httpClient
         * // first === second
         * ```
         */
        val httpClient: SdkHttpClient by lazy {
            UrlConnectionHttpClient.builder().build()
                .apply {
                    ShutdownQueue.register(this)
                }
        }
    }

    /**
     * 기본 동기 HTTP 클라이언트로 Apache 구현을 반환합니다.
     *
     * ## 동작/계약
     * - [Apache.httpClient] 참조를 그대로 반환한다.
     * - 별도 새 인스턴스를 만들지 않는다.
     *
     * ```kotlin
     * val defaultClient = SdkHttpClientProvider.defaultHttpClient
     * // defaultClient === SdkHttpClientProvider.Apache.httpClient
     * ```
     */
    val defaultHttpClient get() = Apache.httpClient
}
