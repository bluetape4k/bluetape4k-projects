package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsAsyncClientBuilder
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder
import java.net.URI

/**
 * [CloudWatchLogsClient], [CloudWatchLogsAsyncClient] 생성을 위한 Factory 입니다.
 */
object CloudWatchLogsClientFactory {

    /**
     * 동기 [CloudWatchLogsClient] 생성을 지원합니다.
     */
    object Sync {

        /**
         * DSL 빌더로 [CloudWatchLogsClient]를 생성합니다.
         */
        inline fun create(
            @BuilderInference builder: CloudWatchLogsClientBuilder.() -> Unit,
        ): CloudWatchLogsClient =
            cloudWatchLogsClient(builder)

        /**
         * endpoint, region, credentials 기반으로 [CloudWatchLogsClient]를 생성합니다.
         */
        fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: CloudWatchLogsClientBuilder.() -> Unit = {},
        ): CloudWatchLogsClient =
            cloudWatchLogsClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    /**
     * 비동기 [CloudWatchLogsAsyncClient] 생성을 지원합니다.
     */
    object Async {

        /**
         * DSL 빌더로 [CloudWatchLogsAsyncClient]를 생성합니다.
         */
        inline fun create(
            @BuilderInference builder: CloudWatchLogsAsyncClientBuilder.() -> Unit,
        ): CloudWatchLogsAsyncClient =
            cloudWatchLogsAsyncClient(builder)

        /**
         * endpoint, region, credentials 기반으로 [CloudWatchLogsAsyncClient]를 생성합니다.
         */
        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: CloudWatchLogsAsyncClientBuilder.() -> Unit = {},
        ): CloudWatchLogsAsyncClient =
            cloudWatchLogsAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }
}
