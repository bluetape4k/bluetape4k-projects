package io.bluetape4k.aws.kms

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import software.amazon.awssdk.services.kms.KmsClientBuilder
import java.net.URI

inline fun kmsClient(
    @BuilderInference builder: KmsClientBuilder.() -> Unit,
): KmsClient =
    KmsClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

inline fun kmsClientOf(
    endpointOverride: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    httpClient: SdkHttpClient = SdkHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: KmsClientBuilder.() -> Unit = {},
): KmsClient = kmsClient {
    endpointOverride(endpointOverride)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(httpClient)

    builder()
}
