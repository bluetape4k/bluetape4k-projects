package io.bluetape4k.aws.sns

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
object SnsClientFactory {

    /**
     * 동기 [SnsClient] 생성을 지원합니다.
     */
    object Sync {

        inline fun create(
            @BuilderInference builder: SnsClientBuilder.() -> Unit,
        ): SnsClient =
            snsClient(builder)

        fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: SnsClientBuilder.() -> Unit = {},
        ): SnsClient =
            snsClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }

    /**
     * 비동기 [SnsAsyncClient] 생성을 지원합니다.
     */
    object Async {

        inline fun create(
            @BuilderInference builder: SnsAsyncClientBuilder.() -> Unit,
        ): SnsAsyncClient =
            snsAsyncClient(builder)

        inline fun create(
            endpointOverride: URI? = null,
            region: Region? = null,
            credentialsProvider: AwsCredentialsProvider? = null,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: SnsAsyncClientBuilder.() -> Unit = {},
        ): SnsAsyncClient =
            snsAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }
}
