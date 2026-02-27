package io.bluetape4k.aws.sns

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder
import java.net.URI

/**
 * [SnsAsyncClient]를 빌드합니다.
 */
inline fun snsAsyncClient(
    @BuilderInference builder: SnsAsyncClientBuilder.() -> Unit,
): SnsAsyncClient =
    SnsAsyncClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * [Region] 기반으로 [SnsAsyncClient]를 생성합니다.
 */
inline fun snsAsyncClientOf(
    region: Region,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: SnsAsyncClientBuilder.() -> Unit = {},
): SnsAsyncClient = snsAsyncClient {
    region(region)
    httpClient(httpClient)

    builder()
}

/**
 * endpoint + credentials 기반으로 [SnsAsyncClient]를 생성합니다.
 */
inline fun snsAsyncClientOf(
    endpoint: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: SnsAsyncClientBuilder.() -> Unit = {},
): SnsAsyncClient = snsAsyncClient {
    endpointOverride(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(httpClient)

    builder()
}
