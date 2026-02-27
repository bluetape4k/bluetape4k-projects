package io.bluetape4k.aws.http

import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.http.async.SdkAsyncHttpClient

/**
 * [SdkAsyncHttpClient] 를 공용으로 사용할 수 있도록 제공하는 Provider 입니다.
 *
 * 참고: [AWS HTTP 클라이언트](https://docs.aws.amazon.com/ko_kr/sdk-for-java/latest/developer-guide/http-configuration.html)
 */
object SdkAsyncHttpClientProvider {

    object Netty {
        @JvmStatic
        val httpClient: SdkAsyncHttpClient by lazy {
            nettyNioAsyncHttpClientOf().apply {
                ShutdownQueue.register(this)
            }
        }
    }

    object AwsCrt {
        @JvmStatic
        val httpClient: SdkAsyncHttpClient by lazy {
            awsCrtAsyncHttpClientOf().apply {
                ShutdownQueue.register(this)
            }
        }
    }

    val defaultHttpClient: SdkAsyncHttpClient get() = Netty.httpClient
}
