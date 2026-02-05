package io.bluetape4k.aws.dynamodb.examples.food.config

import io.bluetape4k.aws.dynamodb.DynamoDbAsyncClient
import io.bluetape4k.aws.dynamodb.enhanced.dynamoDbEnhancedAsyncClientOf
import io.bluetape4k.aws.dynamodb.schema.DynamoDbAsyncTableCreator
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import java.net.URI

@Configuration
class DynamoDbConfig(
    @param:Value($$"${aws.region}") private val awsRegion: String,
    @param:Value($$"${aws.dynamodb.endpoint}") private val endpoint: String,
    private val awsCredentialsProvider: AwsCredentialsProvider,
) {

    companion object: KLoggingChannel()

    @Bean(name = ["amazonDynamoDb"])
    fun dynamoDbAsyncClient(): DynamoDbAsyncClient {
        return DynamoDbAsyncClient {
            endpointOverride(URI.create(endpoint))
            region(Region.of(awsRegion))
            credentialsProvider(awsCredentialsProvider)
        }
    }

    @Bean
    fun dynamoDbEnhancedAsyncClient(asyncClient: DynamoDbAsyncClient): DynamoDbEnhancedAsyncClient {
        return dynamoDbEnhancedAsyncClientOf(asyncClient)
    }

    @Bean
    fun dynamoDbAsyncTableCreator(): DynamoDbAsyncTableCreator {
        return DynamoDbAsyncTableCreator()
    }
}
