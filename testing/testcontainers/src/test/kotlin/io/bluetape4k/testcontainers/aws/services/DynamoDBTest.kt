package io.bluetape4k.testcontainers.aws.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.testcontainers.aws.LocalStackServer
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.internal.client.ExtensionResolver
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import java.net.URI

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DynamoDBTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val TABLE_NAME = "test-table"
    }

    private val dynamodb: LocalStackServer by lazy {
        LocalStackServer.Launcher.localStack.withServices(LocalStackContainer.Service.DYNAMODB)
    }
    private val endpoint: URI get() = dynamodb.getEndpointOverride(LocalStackContainer.Service.DYNAMODB)

    private val client by lazy {
        DynamoDbClient.builder()
            .endpointOverride(endpoint)
            .region(Region.of(dynamodb.region))
            .credentialsProvider(dynamodb.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    private val asyncClient by lazy {
        DynamoDbAsyncClient.builder()
            .endpointOverride(endpoint)
            .region(Region.of(dynamodb.region))
            .credentialsProvider(dynamodb.getCredentialProvider())
            .build()
            .apply {
                ShutdownQueue.register(this)
            }
    }

    private val enhancedAsyncClient by lazy {
        DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(asyncClient)
            .extensions(ExtensionResolver.defaultExtensions())
    }


    @BeforeAll
    fun setup() {
        dynamodb.start()

        val createTableRequest = CreateTableRequest.builder()
            .tableName(TABLE_NAME)
            .attributeDefinitions(
                AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
            )
            .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
            .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(5L).writeCapacityUnits(5L).build())
            .build()
        val createTableResponse = client.createTable(createTableRequest)
        createTableResponse.shouldNotBeNull()
        log.debug { "Table: ${createTableResponse.tableDescription()}" }
    }

    @Test
    @Order(1)
    fun `insert data`() {
        val item = mutableMapOf(
            "id" to AttributeValue.builder().s("1").build(),
            "name" to AttributeValue.builder().s("debop").build(),
            "age" to AttributeValue.builder().n("51").build()
        )

        val response = client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build())
        response.shouldNotBeNull()

        val scanResponse = client.scan(ScanRequest.builder().tableName(TABLE_NAME).build())
        scanResponse.shouldNotBeNull()
        scanResponse.count() shouldBeGreaterOrEqualTo 1
    }
}
