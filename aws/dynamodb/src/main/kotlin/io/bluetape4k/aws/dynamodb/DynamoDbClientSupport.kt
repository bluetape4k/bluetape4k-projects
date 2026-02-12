package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.http.SdkHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder
import java.net.URI

/**
 * [dynamoDbClient]를 빌드해주는 함수입니다.
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
 * @return [dynamoDbClient] 인스턴스
 */
inline fun dynamoDbClient(
    @BuilderInference builder: DynamoDbClientBuilder.() -> Unit,
): DynamoDbClient {
    return DynamoDbClient.builder().apply(builder).build()
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
inline fun dynamoDbClientOf(
    endpoint: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    @BuilderInference builder: DynamoDbClientBuilder.() -> Unit = {},
): DynamoDbClient = dynamoDbClient {
    endpointOverride(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(SdkHttpClientProvider.Apache.apacheHttpClient)

    builder()
}
