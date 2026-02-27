package io.bluetape4k.aws.ses

import io.bluetape4k.aws.auth.LocalAwsCredentialsProvider
import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.aws.http.SdkHttpClientProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.SesAsyncClientBuilder
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.SesClientBuilder
import java.net.URI

/**
 * [SesClient], [SesAsyncClient] 생성을 위한 Factory 입니다.
 */
object SesFactory {

    /**
     * 동기 [SesClient] 생성을 지원합니다.
     */
    object Sync {

        inline fun create(
            @BuilderInference builder: SesClientBuilder.() -> Unit,
        ): SesClient = sesClient(builder)

        inline fun create(
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: SesClientBuilder.() -> Unit = {},
        ): SesClient =
            create {
                endpointOverride(endpointOverride)
                region(region)
                credentialsProvider(credentialsProvider)
                httpClient(httpClient)

                builder()
            }
    }

    /**
     * 비동기 [SesAsyncClient] 생성을 지원합니다.
     */
    object Async {

        inline fun create(
            @BuilderInference builder: SesAsyncClientBuilder.() -> Unit,
        ): SesAsyncClient =
            sesAsyncClient(builder)

        inline fun create(
            endpointOverride: URI,
            region: Region = Region.AP_NORTHEAST_2,
            credentialsProvider: AwsCredentialsProvider = LocalAwsCredentialsProvider,
            asyncHttpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
            @BuilderInference builder: SesAsyncClientBuilder.() -> Unit = {},
        ): SesAsyncClient =
            create {
                endpointOverride(endpointOverride)
                region(region)
                credentialsProvider(credentialsProvider)
                httpClient(asyncHttpClient)
                builder()
            }
    }
}
