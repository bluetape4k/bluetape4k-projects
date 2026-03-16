package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.CloudWatchClientBuilder
import java.net.URI

/**
 * [CloudWatchClient]를 빌드합니다.
 *
 * ```kotlin
 * val client = cloudWatchClient { region(Region.AP_NORTHEAST_2) }
 * ```
 */
inline fun cloudWatchClient(
    builder: CloudWatchClientBuilder.() -> Unit,
): CloudWatchClient =
    CloudWatchClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * [Region] 기반으로 [CloudWatchClient]를 생성합니다.
 *
 * [httpClient]는 기본 HTTP 클라이언트를 사용하며, 생성된 클라이언트는 [ShutdownQueue]에 등록됩니다.
 *
 * ```kotlin
 * val client = cloudWatchClientOf(Region.AP_NORTHEAST_2)
 * ```
 */
inline fun cloudWatchClientOf(
    region: Region,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    builder: CloudWatchClientBuilder.() -> Unit = {},
): CloudWatchClient = cloudWatchClient {
    region(region)
    httpClient(httpClient)

    builder()
}

/**
 * endpoint + credentials 기반으로 [CloudWatchClient]를 생성합니다.
 *
 * nullable 파라미터는 null 이 아닐 때만 builder에 반영됩니다.
 *
 * ```kotlin
 * val client = cloudWatchClientOf(endpoint = URI("http://localhost:4566"))
 * ```
 */
inline fun cloudWatchClientOf(
    endpoint: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    builder: CloudWatchClientBuilder.() -> Unit = {},
): CloudWatchClient = cloudWatchClient {
    endpoint?.let { endpointOverride(it) }
    region?.let { region(it) }
    credentialsProvider?.let { credentialsProvider(it) }
    httpClient(httpClient)

    builder()
}
