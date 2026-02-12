package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsAsyncClient
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsAsyncClientBuilder
import java.net.URI

/**
 * [dynamoDbStreamsAsyncClient]를 빌드해주는 함수입니다.
 * JVM 종료 시 자동으로 자원을 해제합니다
 *
 * ```
 * val dynamoDbStreamsAsyncClient = DynamoDbStreamsAsyncClient {
 *     credentialsProvider(credentialsProvider)
 *     endpointOverride(endpoint)
 *     region(region)
 *     httpClient(SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient)
 * }
 * ```
 * @param builder [DynamoDbStreamsAsyncClientBuilder] 초기화 람다
 * @return [dynamoDbStreamsAsyncClient] 인스턴스
 */
inline fun dynamoDbStreamsAsyncClient(
    @BuilderInference builder: DynamoDbStreamsAsyncClientBuilder.() -> Unit,
): DynamoDbStreamsAsyncClient {
    return DynamoDbStreamsAsyncClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }
}

/**
 * [DynamoDbStreamsAsyncClient]를 빌드해주는 함수입니다.
 * JVM 종료 시 자동으로 자원을 해제합니다
 *
 * ```
 * val dynamoDbStreamsAsyncClient = dynamoDbStreamsAsyncClientOf(
 *    endpoint = endpoint,
 *      region = region,
 *      credentialsProvider = credentialsProvider,
 * ) {
 *    httpClient(SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient)
 * }
 * ```
 * @param endpoint [URI] DynamoDB 엔드포인트
 * @param region [Region] DynamoDB 리전
 * @param credentialsProvider [AwsCredentialsProvider] 자격 증명 제공자
 * @param builder [DynamoDbStreamsAsyncClientBuilder] 초기화 람다
 *
 * @return [DynamoDbStreamsAsyncClient] 인스턴스
 *
 * @see DynamoDbStreamsAsyncClient
 */
inline fun dynamoDbStreamsAsyncClientOf(
    endpoint: URI,
    region: Region,
    credentialsProvider: AwsCredentialsProvider,
    @BuilderInference builder: DynamoDbStreamsAsyncClientBuilder.() -> Unit = {},
): DynamoDbStreamsAsyncClient = dynamoDbStreamsAsyncClient {
    endpointOverride(endpoint)
    region(region)
    credentialsProvider(credentialsProvider)
    httpClient(SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient)

    builder()
}
