package io.bluetape4k.aws.dynamodb.enhanced

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClientExtension
import software.amazon.awssdk.enhanced.dynamodb.internal.client.ExtensionResolver
import software.amazon.awssdk.services.dynamodb.DynamoDbClient


inline fun dynamoDbEnhancedClient(
    @BuilderInference builder: DynamoDbEnhancedClient.Builder.() -> Unit,
): DynamoDbEnhancedClient =
    DynamoDbEnhancedClient.builder().apply(builder).build()

inline fun dynamoDbEnhancedClientOf(
    client: DynamoDbClient,
    @BuilderInference builder: DynamoDbEnhancedClient.Builder.() -> Unit =
        { extensions(ExtensionResolver.defaultExtensions()) },
): DynamoDbEnhancedClient = dynamoDbEnhancedClient {
    dynamoDbClient(client)
    builder()
}

fun dynamoDbEnhancedClientOf(
    client: DynamoDbClient,
    vararg extensions: DynamoDbEnhancedClientExtension = ExtensionResolver.defaultExtensions().toTypedArray(),
): DynamoDbEnhancedClient = dynamoDbEnhancedClient {
    dynamoDbClient(client)
    extensions(*extensions)
}
