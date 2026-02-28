package io.bluetape4k.aws.sns

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.SnsClientBuilder
import java.net.URI

/**
 * [SnsClient]를 빌드합니다.
 */
inline fun snsClient(builder: SnsClientBuilder.() -> Unit): SnsClient =
    SnsClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * [Region] 기반으로 [SnsClient]를 생성합니다.
 */
inline fun snsClientOf(
    region: Region,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: SnsClientBuilder.() -> Unit = {},
): SnsClient = snsClient {
    region(region)
    httpClient(httpClient)

    builder()
}

/**
 * endpoint + credentials 기반으로 [SnsClient]를 생성합니다.
 */
inline fun snsClientOf(
    endpoint: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: SnsClientBuilder.() -> Unit = {},
): SnsClient = snsClient {
    endpoint?.let { endpointOverride(it) }
    region?.let { region(it) }
    credentialsProvider?.let { credentialsProvider(it) }
    httpClient(httpClient)

    builder()
}
