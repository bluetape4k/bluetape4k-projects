package io.bluetape4k.aws.ses

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.SesClientBuilder
import software.amazon.awssdk.services.ses.endpoints.SesEndpointProvider

/**
 * [sesClient]를 빌드합니다.
 *
 * ```
 * val client = SesClient {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 *     region(region)
 * }
 * client.verifyEmailAddress { it.emailAddress(senderEmail) }
 * client.verifyEmailAddress { it.emailAddress(receiverEamil) }
 * client.send(request)
 * ```
 */
inline fun sesClient(
    @BuilderInference builder: SesClientBuilder.() -> Unit,
): SesClient {
    return SesClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }
}

/**
 * [SesClient]를 생성합니다.
 *
 * ```
 * val client = sesClientOf(region) {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 * }
 * client.verifyEmailAddress { it.emailAddress(senderEmail) }
 * client.verifyEmailAddress { it.emailAddress(receiverEamil) }
 * client.send(request)
 * ```
 */
inline fun sesClientOf(
    region: Region,
    @BuilderInference builder: SesClientBuilder.() -> Unit = {},
): SesClient = sesClient {
    region(region)
    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)

    builder()
}

/**
 * [SesClient]를 생성합니다.
 *
 * ```
 * val client = sesClientOf(endpointProvider) {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 * }
 * client.verifyEmailAddress { it.emailAddress(senderEmail) }
 * client.verifyEmailAddress { it.emailAddress(receiverEamil) }
 * client.send(request)
 * ```
 */
inline fun sesClientOf(
    endpointProvider: SesEndpointProvider,
    @BuilderInference builder: SesClientBuilder.() -> Unit = {},
): SesClient = sesClient {
    endpointProvider(endpointProvider)
    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)

    builder()
}
