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
 * endpoint + credentials 기반으로 [SnsAsyncClient]를 생성합니다.
 */
inline fun snsAsyncClientOf(
    endpoint: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: SnsAsyncClientBuilder.() -> Unit = {},
): SnsAsyncClient = snsAsyncClient {
    endpoint?.let { endpointOverride(it) }
    region?.let { region(it) }
    credentialsProvider?.let { credentialsProvider(it) }

    httpClient(httpClient)

    builder()
}
