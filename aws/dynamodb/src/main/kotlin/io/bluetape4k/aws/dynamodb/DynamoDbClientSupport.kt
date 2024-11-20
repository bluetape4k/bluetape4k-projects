package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClient
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsClientBuilder
import java.net.URI

/**
 * [DynamoDbClient]를 빌드해주는 함수입니다.
 * JVM 종료 시 자동으로 자원을 해제합니다
 *
 * ```
 * val dynamoDbClient = DynamoDbClient {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 *     region(region)
 *     httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)
 * }
 * ```
 *
 * @return [DynamoDbClient] 인스턴스
 */
inline fun DynamoDbClient(
    initializer: DynamoDbClientBuilder.() -> Unit,
): DynamoDbClient {
    return DynamoDbClient.builder().apply(initializer).build()
        .apply {
            ShutdownQueue.register(this)
        }
}

/**
 * [DynamoDbClient]를 빌드해주는 함수입니다.
 *
 * ```
 * val dynamoDbClient = dynamoDbClientOf(
 *    endpoint = endpoint,
 *    region = region,
 *    credentialsProvider = credentialsProvider,
 * ) {
 *    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)
 * }
 * ```
 *
 * @return [DynamoDbClient] 인스턴스
 * @see [DynamoDbClient]
 */
fun dynamoDbClientOf(
    endpoint: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    initializer: DynamoDbClientBuilder.() -> Unit = {},
): DynamoDbClient = DynamoDbClient {
    endpointOverride(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)

    initializer()
}

/**
 * [DynamoDbStreamsClient]를 빌드해주는 함수입니다.
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
 * @return [DynamoDbStreamsClient] 인스턴스
 */
inline fun DynamoDbStreamsClient(
    initializer: DynamoDbStreamsClientBuilder.() -> Unit,
): DynamoDbStreamsClient {
    return DynamoDbStreamsClient.builder().apply(initializer).build()
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
fun dynamoDbStreamsClientOf(
    endpoint: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    initializer: DynamoDbStreamsClientBuilder.() -> Unit = {},
): DynamoDbStreamsClient = DynamoDbStreamsClient {
    endpointOverride(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)

    initializer()
}
