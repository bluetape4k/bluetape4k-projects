package io.bluetape4k.aws.dynamodb

import io.bluetape4k.aws.http.SdkAsyncHttpClientProvider
import io.bluetape4k.utils.ShutdownQueue
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder
import java.net.URI

/**
 * [dynamoDbAsyncClient]를 빌드해주는 함수입니다.
 * JVM 종료 시 자동으로 자원을 해제합니다
 *
 * ```
 * val dynamoDbAsyncClient = DynamoDbAsyncClient {
 *      credentialsProvider(credentialsProvider)
 *      endpointOverride(endpoint)
 *      region(region)
 *      httpClient(SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient)
 * }
 * ```
 * @param builder [DynamoDbAsyncClientBuilder] 초기화 람다
 * @return [dynamoDbAsyncClient] 인스턴스
 */
inline fun dynamoDbAsyncClient(
    @BuilderInference builder: DynamoDbAsyncClientBuilder.() -> Unit,
): DynamoDbAsyncClient =
    DynamoDbAsyncClient.builder().apply(builder).build()
        .apply {
            ShutdownQueue.register(this)
        }

/**
 * [DynamoDbAsyncClient]를 빌드해주는 함수입니다.
 *
 * ```
 * val dynamoDbAsyncClient = dynamoDbAsyncClientOf(
 *     endpoint = endpoint,
 *     region = region,
 *     credentialsProvider = credentialsProvider,
 * ) {
 *   httpClient(SdkAsyncHttpClientProvider.Netty.nettyNioAsyncHttpClient)
 * }
 * ```
 * @param endpoint [URI] DynamoDB 엔드포인트
 * @param region [Region] DynamoDB 리전
 * @param credentialsProvider [AwsCredentialsProvider] 자격 증명 제공자
 * @param builder [DynamoDbAsyncClientBuilder] 초기화 람다
 *
 * @return [DynamoDbAsyncClient] 인스턴스
 * @see DynamoDbAsyncClient
 */
inline fun dynamoDbAsyncClientOf(
    endpointOverride: URI? = null,
    region: Region? = null,
    credentialsProvider: AwsCredentialsProvider? = null,
    httpClient: SdkAsyncHttpClient = SdkAsyncHttpClientProvider.defaultHttpClient,
    @BuilderInference builder: DynamoDbAsyncClientBuilder.() -> Unit = {},
): DynamoDbAsyncClient = dynamoDbAsyncClient {
    endpointOverride?.let { endpointOverride(it) }
    region?.let { region(it) }
    credentialsProvider?.let { credentialsProvider(it) }
    httpClient(httpClient)

    builder()
}
