package io.bluetape4k.testcontainers.aws

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.AbstractContainerTest
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest

/**
 * LocalStack을 사용한 AWS DynamoDB 서비스 예제 테스트.
 *
 * 각 테스트는 독립적인 [LocalStackServer]를 사용하여 격리된 환경에서 실행됩니다.
 */
class LocalStackDynamoDbServiceTest: AbstractContainerTest() {

    companion object: KLogging() {
        private const val TABLE_NAME = "test-users"
        private const val PK = "userId"   // 파티션 키
        private const val SK = "createdAt" // 정렬 키
    }

    private fun buildDynamoDbClient(server: LocalStackServer): DynamoDbClient =
        DynamoDbClient.builder()
            .endpointOverride(server.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
            .region(Region.of(server.region))
            .credentialsProvider(server.getCredentialProvider())
            .build()
            .apply { ShutdownQueue.register(this) }

    private fun DynamoDbClient.createTestTable(tableName: String = TABLE_NAME) {
        createTable(
            CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(
                    KeySchemaElement.builder().attributeName(PK).keyType(KeyType.HASH).build(),
                    KeySchemaElement.builder().attributeName(SK).keyType(KeyType.RANGE).build(),
                )
                .attributeDefinitions(
                    AttributeDefinition.builder().attributeName(PK).attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName(SK).attributeType(ScalarAttributeType.S).build(),
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build()
        )
    }

    private fun itemKey(userId: String, createdAt: String) = mapOf(
        PK to AttributeValue.builder().s(userId).build(),
        SK to AttributeValue.builder().s(createdAt).build(),
    )

    @Test
    fun `DynamoDB 테이블 생성 후 아이템 Put, Get, Delete CRUD`() {
        LocalStackServer().withServices(LocalStackContainer.Service.DYNAMODB).use { server ->
            server.start()
            val dynamoDb = buildDynamoDbClient(server)
            dynamoDb.createTestTable()

            val userId = "user-001"
            val createdAt = "2025-01-01T00:00:00Z"

            // 아이템 저장 (Put)
            dynamoDb.putItem(
                PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(
                        mapOf(
                            PK to AttributeValue.builder().s(userId).build(),
                            SK to AttributeValue.builder().s(createdAt).build(),
                            "name" to AttributeValue.builder().s("홍길동").build(),
                            "email" to AttributeValue.builder().s("hong@example.com").build(),
                            "age" to AttributeValue.builder().n("30").build(),
                        )
                    )
                    .build()
            )

            // 아이템 조회 (Get)
            val item = dynamoDb.getItem(
                GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(itemKey(userId, createdAt))
                    .build()
            ).item()

            item.shouldNotBeNull()
            item[PK]?.s() shouldBeEqualTo userId
            item["name"]?.s() shouldBeEqualTo "홍길동"
            item["email"]?.s() shouldBeEqualTo "hong@example.com"
            item["age"]?.n() shouldBeEqualTo "30"

            // 아이템 수정 (Update)
            dynamoDb.updateItem(
                UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(itemKey(userId, createdAt))
                    .updateExpression("SET #name = :newName, age = :newAge")
                    .expressionAttributeNames(mapOf("#name" to "name"))
                    .expressionAttributeValues(
                        mapOf(
                            ":newName" to AttributeValue.builder().s("김철수").build(),
                            ":newAge" to AttributeValue.builder().n("25").build(),
                        )
                    )
                    .build()
            )

            val updated = dynamoDb.getItem(
                GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(itemKey(userId, createdAt))
                    .build()
            ).item()
            updated["name"]?.s() shouldBeEqualTo "김철수"
            updated["age"]?.n() shouldBeEqualTo "25"

            // 아이템 삭제 (Delete)
            dynamoDb.deleteItem(
                DeleteItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(itemKey(userId, createdAt))
                    .build()
            )

            // 삭제 후 조회 - 빈 결과 확인
            val afterDelete = dynamoDb.getItem(
                GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(itemKey(userId, createdAt))
                    .build()
            ).item()
            afterDelete.isEmpty().shouldBeTrue()
        }
    }

    @Test
    fun `DynamoDB 다수 아이템 저장 후 Scan 및 Query 수행`() {
        LocalStackServer().withServices(LocalStackContainer.Service.DYNAMODB).use { server ->
            server.start()
            val dynamoDb = buildDynamoDbClient(server)
            dynamoDb.createTestTable()

            val userId = "user-batch"
            val itemCount = 5

            // 같은 파티션 키로 여러 아이템 저장
            repeat(itemCount) { i ->
                dynamoDb.putItem(
                    PutItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .item(
                            mapOf(
                                PK to AttributeValue.builder().s(userId).build(),
                                SK to AttributeValue.builder().s("2025-01-0${i + 1}T00:00:00Z").build(),
                                "seq" to AttributeValue.builder().n("$i").build(),
                            )
                        )
                        .build()
                )
            }

            // 전체 Scan
            val scanCount = dynamoDb.scan(
                ScanRequest.builder().tableName(TABLE_NAME).build()
            ).count()
            scanCount shouldBeEqualTo itemCount

            // 특정 파티션 키로 Query
            val queryCount = dynamoDb.query(
                QueryRequest.builder()
                    .tableName(TABLE_NAME)
                    .keyConditionExpression("#pk = :uid")
                    .expressionAttributeNames(mapOf("#pk" to PK))
                    .expressionAttributeValues(
                        mapOf(":uid" to AttributeValue.builder().s(userId).build())
                    )
                    .build()
            ).count()
            queryCount shouldBeEqualTo itemCount
        }
    }

    @Test
    fun `DynamoDB 조건부 Write로 중복 방지`() {
        LocalStackServer().withServices(LocalStackContainer.Service.DYNAMODB).use { server ->
            server.start()
            val dynamoDb = buildDynamoDbClient(server)
            dynamoDb.createTestTable()

            val key = itemKey("user-cond", "2025-01-01T00:00:00Z")

            // 첫 번째 Put (attribute_not_exists 조건)
            dynamoDb.putItem(
                PutItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .item(key + mapOf("name" to AttributeValue.builder().s("최초 데이터").build()))
                    .conditionExpression("attribute_not_exists($PK)")
                    .build()
            )

            // 중복 Put 시도 → ConditionalCheckFailedException 발생 확인
            val duplicate = runCatching {
                dynamoDb.putItem(
                    PutItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .item(key + mapOf("name" to AttributeValue.builder().s("덮어쓰기 시도").build()))
                        .conditionExpression("attribute_not_exists($PK)")
                        .build()
                )
            }
            duplicate.isFailure.shouldBeTrue()

            // 원본 데이터가 그대로인지 확인
            val item = dynamoDb.getItem(
                GetItemRequest.builder().tableName(TABLE_NAME).key(key).build()
            ).item()
            item["name"]?.s() shouldBeEqualTo "최초 데이터"
        }
    }
}
