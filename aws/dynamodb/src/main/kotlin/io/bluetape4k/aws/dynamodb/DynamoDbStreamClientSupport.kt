package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.http.SdkHttpClientProvider
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClientBuilder
import java.net.URI

/**
 * [dynamoDbStreamsClient]를 빌드해주는 함수입니다.
 *
 * ```
 * val dynamoDbStreamsClient = DynamoDbStreamsClient {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 *     region(region)
 *     httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)
 * }
 * ```
 *
 * @return [dynamoDbStreamsClient] 인스턴스
 */
inline fun dynamoDbStreamsClient(
    @BuilderInference builder: DynamoDbStreamsClientBuilder.() -> Unit,
): DynamoDbStreamsClient {
    return DynamoDbStreamsClient.builder().apply(builder).build()
}

/**
 * [DynamoDbStreamsClient]를 빌드해주는 함수입니다.
 *
 * ```
 * val dynamoDbStreamsClient = dynamoDbStreamsClientOf(
 *    endpoint = endpoint,
 *    region = region,
 *    credentialsProvider = credentialsProvider,
 * ) {
 *    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)
 * }
 * ```
 *
 * @return [DynamoDbStreamsClient] 인스턴스
 * @see [DynamoDbStreamsClient]
 */
inline fun dynamoDbStreamsClientOf(
    endpoint: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    @BuilderInference builder: DynamoDbStreamsClientBuilder.() -> Unit = {},
): DynamoDbStreamsClient = dynamoDbStreamsClient {
    endpointOverride(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)

    builder()
}
