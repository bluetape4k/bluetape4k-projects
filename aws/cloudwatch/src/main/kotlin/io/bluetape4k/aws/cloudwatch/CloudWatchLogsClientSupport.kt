package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder
import java.net.URI

/**
 * [CloudWatchLogsClient]를 빌드합니다.
 *
 * ```kotlin
 * val client = cloudWatchLogsClient { region(Region.AP_NORTHEAST_2) }
 * ```
 */
inline fun cloudWatchLogsClient(
    @BuilderInference builder: CloudWatchLogsClientBuilder.() -> Unit,
): CloudWatchLogsClient =
    CloudWatchLogsClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * [Region] 기반으로 [CloudWatchLogsClient]를 생성합니다.
 *
 * [httpClient]는 기본 HTTP 클라이언트를 사용하며, 생성된 클라이언트는 [ShutdownQueue]에 등록됩니다.
 *
 * ```kotlin
 * val client = cloudWatchLogsClientOf(Region.AP_NORTHEAST_2)
 * ```
 */
inline fun cloudWatchLogsClientOf(
    region: Region,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: CloudWatchLogsClientBuilder.() -> Unit = {},
): CloudWatchLogsClient = cloudWatchLogsClient {
    region(region)
    httpClient(httpClient)

    builder()
}

/**
 * endpoint + credentials 기반으로 [CloudWatchLogsClient]를 생성합니다.
 *
 * nullable 파라미터는 null 이 아닐 때만 builder에 반영됩니다.
 *
 * ```kotlin
 * val client = cloudWatchLogsClientOf(endpoint = URI("http://localhost:4566"))
 * ```
 */
inline fun cloudWatchLogsClientOf(
    endpoint: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: CloudWatchLogsClientBuilder.() -> Unit = {},
): CloudWatchLogsClient = cloudWatchLogsClient {
    endpoint?.let { endpointOverride(it) }
    region?.let { region(it) }
    credentialsProvider?.let { credentialsProvider(it) }
    httpClient(httpClient)

    builder()
}
