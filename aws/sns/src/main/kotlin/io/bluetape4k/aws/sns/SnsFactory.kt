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
object SnsFactory {

    /**
     * 동기 [SnsClient] 생성을 지원합니다.
     */
    object Sync {

        inline fun create(
            @BuilderInference builder: SnsClientBuilder.() -> Unit,
        ): SnsClient =
            snsClient(builder)

        fun create(
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            httpClient: SdkHttpClient = SdkHttpClientProvider.Apache.apacheHttpClient,
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
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient,
            @BuilderInference builder: SnsAsyncClientBuilder.() -> Unit = {},
        ): SnsAsyncClient =
            snsAsyncClientOf(endpointOverride, region, credentialsProvider, httpClient, builder)
    }
}
