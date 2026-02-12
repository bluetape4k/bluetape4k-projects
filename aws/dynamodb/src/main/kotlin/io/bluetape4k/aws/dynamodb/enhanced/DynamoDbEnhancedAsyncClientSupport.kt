package io.bluetape4k.aws.dynamodb.enhanced

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension
import software.amazon.awssdk.enhanced.dynamodb.internal.client.ExtensionResolver
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

/**
 * [DynamoDbEnhancedAsyncClient] 를 생성합니다.
 *
 * ```
 * val client = dynamoDbEnhancedAsyncClient {
 *    dynamoDbClient(DynamoDbAsyncClient.create())
 * }
 * ```
 *
 * @param builder [DynamoDbEnhancedAsyncClient.Builder] 를 초기화하는 람다 함수
 * @return [DynamoDbEnhancedAsyncClient] instance
 */
inline fun dynamoDbEnhancedAsyncClient(
    @BuilderInference builder: DynamoDbEnhancedAsyncClient.Builder.() -> Unit,
): DynamoDbEnhancedAsyncClient {
    return DynamoDbEnhancedAsyncClient.builder().apply(builder).build()
}

/**
 * [DynamoDbEnhancedAsyncClient] 를 생성합니다.
 *
 * ```
 * val client = dynamoDbEnhancedAsyncClientOf(DynamoDbAsyncClient.create()) {
 *   extensions(ExtensionResolver.defaultExtensions())
 * }
 * ```
 *
 * @param client [DynamoDbAsyncClient] instance
 * @param builder [DynamoDbEnhancedAsyncClient.Builder] 를 초기화하는 람다 함수
 * @return [DynamoDbEnhancedAsyncClient] instance
 */
inline fun dynamoDbEnhancedAsyncClientOf(
    client: DynamoDbAsyncClient,
    @BuilderInference builder: DynamoDbEnhancedAsyncClient.Builder.() -> Unit =
        { extensions(ExtensionResolver.defaultExtensions()) },
): DynamoDbEnhancedAsyncClient =
    dynamoDbEnhancedAsyncClient {
        dynamoDbClient(client)
        builder()
    }

/**
 * [DynamoDbEnhancedAsyncClient] 를 생성합니다.
 *
 * ```
 * val client = dynamoDbEnhancedAsyncClientOf(
 *      DynamoDbAsyncClient.create(),
 *      *ExtensionResolver.defaultExtensions().toTypedArray()
 * )
 * ```
 *
 * @param client [DynamoDbAsyncClient] instance
 * @param extensions [DynamoDbEnhancedClientExtension] extensions
 * @return [DynamoDbEnhancedAsyncClient] instance
 */
fun dynamoDbEnhancedAsyncClientOf(
    client: DynamoDbAsyncClient,
    vararg extensions: DynamoDbEnhancedClientExtension = ExtensionResolver.defaultExtensions().toTypedArray(),
): DynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient {
    dynamoDbClient(client)
    extensions(*extensions)
}
