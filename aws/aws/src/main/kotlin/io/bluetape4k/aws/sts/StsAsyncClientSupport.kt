package io.bluetape4k.aws.sts

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsAsyncClient
import software.amazon.awssdk.services.sts.StsAsyncClientBuilder
import java.net.URI

/**
 * [StsAsyncClient]를 빌드합니다.
 *
 * ```kotlin
 * val client = stsAsyncClient { region(Region.AP_NORTHEAST_2) }
 * // client == StsAsyncClient 인스턴스
 * ```
 */
inline fun stsAsyncClient(
    builder: StsAsyncClientBuilder.() -> Unit,
): StsAsyncClient =
    StsAsyncClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * endpoint + credentials 기반으로 [StsAsyncClient]를 생성합니다.
 *
 * nullable 파라미터는 null 이 아닐 때만 builder에 반영됩니다.
 *
 * ```kotlin
 * val client = stsAsyncClientOf(endpoint = URI("http://localhost:4566"))
 * // client == StsAsyncClient 인스턴스
 * ```
 */
inline fun stsAsyncClientOf(
    endpoint: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
    builder: StsAsyncClientBuilder.() -> Unit = {},
): StsAsyncClient = stsAsyncClient {
    endpoint?.let { endpointOverride(it) }
    region?.let { region(it) }
    credentialsProvider?.let { credentialsProvider(it) }
    httpClient(httpClient)

    builder()
}
