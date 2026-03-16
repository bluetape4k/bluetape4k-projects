package io.bluetape4k.aws.dynamodb.enhanced

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension
import software.amazon.awssdk.enhanced.dynamodb.internal.client.ExtensionResolver
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

/**
 * [DynamoDbEnhancedClient.Builder]를 구성해 [DynamoDbEnhancedClient]를 생성합니다.
 *
 * ```kotlin
 * val enhanced = dynamoDbEnhancedClient {
 *     dynamoDbClient(DynamoDbClient.create())
 * }
 *
 * check(enhanced != null)
 * ```
 */
inline fun dynamoDbEnhancedClient(
    @BuilderInference builder: DynamoDbEnhancedClient.Builder.() -> Unit,
): DynamoDbEnhancedClient =
    DynamoDbEnhancedClient.builder().apply(builder).build()

/**
 * 기존 [DynamoDbClient]를 사용해 [DynamoDbEnhancedClient]를 생성합니다.
 *
 * 기본 builder는 AWS 기본 extension 집합([ExtensionResolver.defaultExtensions])을 적용합니다.
 *
 * ```kotlin
 * val baseClient = DynamoDbClient.create()
 * val enhanced = dynamoDbEnhancedClientOf(baseClient)
 *
 * check(enhanced != null)
 * ```
 */
inline fun dynamoDbEnhancedClientOf(
    client: DynamoDbClient,
    @BuilderInference builder: DynamoDbEnhancedClient.Builder.() -> Unit =
        { extensions(ExtensionResolver.defaultExtensions()) },
): DynamoDbEnhancedClient = dynamoDbEnhancedClient {
    dynamoDbClient(client)
    builder()
}

/**
 * [DynamoDbEnhancedClientExtension] 목록을 명시해 [DynamoDbEnhancedClient]를 생성합니다.
 *
 * ```kotlin
 * val baseClient = DynamoDbClient.create()
 * val enhanced = dynamoDbEnhancedClientOf(baseClient, *ExtensionResolver.defaultExtensions().toTypedArray())
 *
 * check(enhanced != null)
 * ```
 */
fun dynamoDbEnhancedClientOf(
    client: DynamoDbClient,
    vararg extensions: DynamoDbEnhancedClientExtension = ExtensionResolver.defaultExtensions().toTypedArray(),
): DynamoDbEnhancedClient = dynamoDbEnhancedClient {
    dynamoDbClient(client)
    extensions(*extensions)
}
