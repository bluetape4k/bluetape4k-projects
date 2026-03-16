package io.bluetape4k.aws.kinesis

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder
import java.net.URI

/**
 * [KinesisClient]를 빌드합니다.
 *
 * ```kotlin
 * val client = kinesisClient { region(Region.AP_NORTHEAST_2) }
 * ```
 */
inline fun kinesisClient(
    builder: KinesisClientBuilder.() -> Unit,
): KinesisClient =
    KinesisClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * [Region] 기반으로 [KinesisClient]를 생성합니다.
 *
 * [httpClient]는 기본 HTTP 클라이언트를 사용하며, 생성된 클라이언트는 [ShutdownQueue]에 등록됩니다.
 *
 * ```kotlin
 * val client = kinesisClientOf(Region.AP_NORTHEAST_2)
 * ```
 */
inline fun kinesisClientOf(
    region: Region,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    builder: KinesisClientBuilder.() -> Unit = {},
): KinesisClient = kinesisClient {
    region(region)
    httpClient(httpClient)

    builder()
}

/**
 * endpoint + credentials 기반으로 [KinesisClient]를 생성합니다.
 *
 * nullable 파라미터는 null 이 아닐 때만 builder에 반영됩니다.
 *
 * ```kotlin
 * val client = kinesisClientOf(endpoint = URI("http://localhost:4566"))
 * ```
 */
inline fun kinesisClientOf(
    endpoint: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    builder: KinesisClientBuilder.() -> Unit = {},
): KinesisClient = kinesisClient {
    endpoint?.let { endpointOverride(it) }
    region?.let { region(it) }
    credentialsProvider?.let { credentialsProvider(it) }
    httpClient(httpClient)

    builder()
}
