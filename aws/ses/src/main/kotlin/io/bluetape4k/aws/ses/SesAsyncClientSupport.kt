package io.bluetape4k.aws.ses

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.nettyNioAsyncHttpClientOf
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.SesAsyncClientBuilder
import software.amazon.awssdk.services.ses.endpoints.SesEndpointProvider

/**
 * [sesAsyncClient]를 빌드합니다.
 *
 * ```
 * val client = SesAsyncClient {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 *     region(region)
 * }
 * val response = client.send(request).await()
 * ```
 *
 * @param builder [SesAsyncClientBuilder]를 이용한 초기화 람다
 * @return [sesAsyncClient] 인스턴스
 */
inline fun sesAsyncClient(
    @BuilderInference builder: SesAsyncClientBuilder.() -> Unit,
): SesAsyncClient {
    return SesAsyncClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }
}

/**
 * [SesAsyncClient]를 생성합니다.
 *
 * ```
 * val client = sesAsyncClientOf(region) {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 * }
 * val response = client.send(request).await()
 * ```
 *
 * @param region [Region] 지역
 * @param builder [SesAsyncClientBuilder]를 이용한 초기화 람다
 * @return [SesAsyncClient] 인스턴스
 */
inline fun sesAsyncClientOf(
    region: Region,
    @BuilderInference builder: SesAsyncClientBuilder.() -> Unit = {},
): SesAsyncClient = sesAsyncClient {
    region(region)
    httpClient(SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient)

    builder()
}

/**
 * [SesAsyncClient]를 생성합니다.
 *
 * ```
 * val client = sesAsyncClientOf(endpointProvider) {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 * }
 * val response = client.send(request).await()
 * ```
 *
 * @param endpointProvider [SesEndpointProvider] 엔드포인트 제공자
 * @param builder [SesAsyncClientBuilder]를 이용한 초기화 람다
 * @return [SesAsyncClient] 인스턴스
 */
inline fun sesAsyncClientOf(
    endpointProvider: SesEndpointProvider,
    @BuilderInference builder: SesAsyncClientBuilder.() -> Unit = {},
): SesAsyncClient = sesAsyncClient {
    endpointProvider(endpointProvider)
    httpClient(nettyNioAsyncHttpClientOf())

    builder()
}
