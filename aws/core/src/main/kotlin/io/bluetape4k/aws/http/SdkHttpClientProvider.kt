package io.bluetape4k.aws.http

import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient

/**
 * [SdkHttpClient] 를 공용으로 사용할 수 있도록 제공하는 Provider 입니다.
 *
 * 참고: [AWS HTTP 클라이언트](https://docs.aws.amazon.com/ko_kr/sdk-for-java/latest/developer-guide/http-configuration.html)
 */
object SdkHttpClientProvider {

    object Apache {

        val httpClient: SdkHttpClient by lazy {
            ApacheHttpClient.builder().build()
                .apply {
                    ShutdownQueue.register(this)
                }
        }
    }

    object UrlConnection {

        val httpClient: SdkHttpClient by lazy {
            UrlConnectionHttpClient.builder().build()
                .apply {
                    ShutdownQueue.register(this)
                }
        }
    }

    val defaultHttpClient get() = Apache.httpClient
}
