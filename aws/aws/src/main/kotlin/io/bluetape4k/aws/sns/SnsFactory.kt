package io.bluetape4k.aws.sns

import io.bluetape4k.aws.auth.LocalAwsCredentialsProvider
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.SnsClientBuilder
import java.net.URI

/**
 * [SnsClient], [SnsAsyncClient] 생성을 위한 Factory 입니다.
 */
@Deprecated("use SnsClientFactory instead", ReplaceWith("SnsClientFactory"))
object SnsFactory {

    /**
     * 동기 [SnsClient] 생성을 지원합니다.
     */
    object Sync {

        /**
         * DSL 빌더로 [SnsClient]를 생성합니다.
         *
         * ```kotlin
         * val client = SnsFactory.Sync.create { region(Region.AP_NORTHEAST_2) }
         * // client != null
         * ```
         */
        inline fun create(
            builder: SnsClientBuilder.() -> Unit,
        ): SnsClient =
            snsClient(builder)

        /**
         * endpoint, region, credentials 기반으로 [SnsClient]를 생성합니다.
         *
         * ```kotlin
         * val client = SnsFactory.Sync.create(
         *     endpointOverride = URI.create("http://localhost:4566"),
         *     region = Region.AP_NORTHEAST_2
         * )
         * // client != null
         * ```
         */
        fun create(
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            builder: SnsClientBuilder.() -> Unit = {},
        ): SnsClient =
            snsClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    /**
     * 비동기 [SnsAsyncClient] 생성을 지원합니다.
     */
    object Async {

        /**
         * DSL 빌더로 [SnsAsyncClient]를 생성합니다.
         *
         * ```kotlin
         * val client = SnsFactory.Async.create { region(Region.AP_NORTHEAST_2) }
         * // client != null
         * ```
         */
        inline fun create(
            builder: SnsAsyncClientBuilder.() -> Unit,
        ): SnsAsyncClient =
            snsAsyncClient(builder)

        /**
         * endpoint, region, credentials 기반으로 [SnsAsyncClient]를 생성합니다.
         *
         * ```kotlin
         * val client = SnsFactory.Async.create(
         *     endpointOverride = URI.create("http://localhost:4566"),
         *     region = Region.AP_NORTHEAST_2
         * )
         * // client != null
         * ```
         */
        inline fun create(
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            builder: SnsAsyncClientBuilder.() -> Unit = {},
        ): SnsAsyncClient =
            snsAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }
}
