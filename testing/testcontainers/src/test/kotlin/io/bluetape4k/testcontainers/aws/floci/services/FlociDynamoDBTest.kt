package io.bluetape4k.testcontainers.aws.floci.services

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.aws.floci.AbstractFlociServiceTest
import io.bluetape4k.testcontainers.aws.getCredentialProvider
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeAction
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.dynamodb.model.ScanRequest

/**
 * [io.bluetape4k.testcontainers.aws.FlociServer]를 사용한 DynamoDB 서비스 통합 테스트.
 *
 * LocalStack 기반 [io.bluetape4k.testcontainers.aws.services.DynamoDBTest]에 대응합니다.
 *
 * > **알려진 제한사항**: Floci #587 — GSI Query 페이지네이션 무한루프 버그로 인해
 * > GSI 관련 테스트는 포함하지 않습니다.
 */
@Suppress("DEPRECATION")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class FlociDynamoDBTest : AbstractFlociServiceTest() {

    companion object : KLogging() {
        private val TABLE_NAME = "test-table-${System.currentTimeMillis()}"
    }

    private val client: DynamoDbClient by lazy {
        DynamoDbClient.builder()
            .endpointOverride(floci.awsEndpoint)
            .region(Region.of(floci.regionName))
            .credentialsProvider(floci.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }
    }

    @BeforeAll
    fun setup() {
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

    @Test
    @Order(2)
    fun `get item`() {
        val key = mutableMapOf(
            "id" to AttributeValue.builder().s("1").build()
        )
        val response = client.getItem { it.tableName(TABLE_NAME).key(key) }
        response.shouldNotBeNull()
        response.hasItem().shouldBeTrue()
        log.debug { "Item: ${response.item()}" }
        response.item()["name"]?.s() shouldBeEqualTo "debop"
    }

    @Test
    @Order(3)
    fun `update item`() {
        val key = mutableMapOf(
            "id" to AttributeValue.builder().s("1").build()
        )
        val updates = mutableMapOf(
            "age" to AttributeValueUpdate.builder()
                .value(AttributeValue.builder().n("52").build())
                .action(AttributeAction.PUT)
                .build()
        )
        val response = client.updateItem {
            it.tableName(TABLE_NAME).key(key).attributeUpdates(updates)
        }
        log.debug { "UpdateItem HTTP status: ${response.sdkHttpResponse().statusCode()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(4)
    fun `delete item`() {
        val key = mutableMapOf(
            "id" to AttributeValue.builder().s("1").build()
        )
        val response = client.deleteItem { it.tableName(TABLE_NAME).key(key) }
        log.debug { "DeleteItem HTTP status: ${response.sdkHttpResponse().statusCode()}" }
        response.sdkHttpResponse().isSuccessful.shouldBeTrue()
    }

    @Test
    @Order(5)
    fun `list tables`() {
        val response = client.listTables()
        log.debug { "Tables: ${response.tableNames()}" }
        response.tableNames() shouldContain TABLE_NAME
    }
}
