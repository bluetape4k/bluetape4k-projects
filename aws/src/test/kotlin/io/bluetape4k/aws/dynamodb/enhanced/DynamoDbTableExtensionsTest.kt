package io.bluetape4k.aws.dynamodb.enhanced

import io.bluetape4k.aws.dynamodb.AbstractDynamodbTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import java.util.*

class DynamoDbTableExtensionsTest: AbstractDynamodbTest() {
    @DynamoDbBean
    data class TestEntity(
        @get:DynamoDbPartitionKey
        var id: String = "",
        var name: String = "",
        var age: Int = 0,
    )

    @Test
    fun `getItem by partition key should return item`() = runSuspendIO {
        val tableName = "sync-test-${UUID.randomUUID()}"

        // 테이블 생성
        client.createTable { builder ->
            builder.tableName(tableName)
            builder.attributeDefinitions(
                AttributeDefinition
                    .builder()
                    .attributeName("id")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
            )
            builder.keySchema(
                KeySchemaElement
                    .builder()
                    .attributeName("id")
                    .keyType(KeyType.HASH)
                    .build(),
            )
            builder.provisionedThroughput(
                ProvisionedThroughput
                    .builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build(),
            )
        }

        // EnhancedClient는 동기 버전을 생성
        val enhancedClient =
            software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
                .builder()
                .dynamoDbClient(client)
                .build()
        val table = enhancedClient.table<TestEntity>(tableName)

        val entity = TestEntity(UUID.randomUUID().toString(), "John", 30)
        table.putItem(entity)

        val result = table.getItem(entity.id)

        result.shouldNotBeNull()
        result.id shouldBeEqualTo entity.id
        result.name shouldBeEqualTo entity.name
        result.age shouldBeEqualTo entity.age

        // cleanup
        client.deleteTable { it.tableName(tableName) }
    }

    @Test
    fun `getItem with non-existent key should return null`() = runSuspendIO {
        val tableName = "sync-test-2-${UUID.randomUUID()}"

        // 테이블 생성
        client.createTable { builder ->
            builder.tableName(tableName)
            builder.attributeDefinitions(
                AttributeDefinition
                    .builder()
                    .attributeName("id")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
            )
            builder.keySchema(
                KeySchemaElement
                    .builder()
                    .attributeName("id")
                    .keyType(KeyType.HASH)
                    .build(),
            )
            builder.provisionedThroughput(
                ProvisionedThroughput
                    .builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build(),
            )
        }

        val enhancedClient =
            software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
                .builder()
                .dynamoDbClient(client)
                .build()
        val table = enhancedClient.table<TestEntity>(tableName)

        val result = table.getItem("non-existent-id")
        result.shouldBeNull()

        // cleanup
        client.deleteTable { it.tableName(tableName) }
    }

    @Test
    fun `deleteItem should remove item`() = runSuspendIO {
        val tableName = "sync-test-3-${UUID.randomUUID()}"

        // 테이블 생성
        client.createTable { builder ->
            builder.tableName(tableName)
            builder.attributeDefinitions(
                AttributeDefinition
                    .builder()
                    .attributeName("id")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
            )
            builder.keySchema(
                KeySchemaElement
                    .builder()
                    .attributeName("id")
                    .keyType(KeyType.HASH)
                    .build(),
            )
            builder.provisionedThroughput(
                ProvisionedThroughput
                    .builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build(),
            )
        }

        val enhancedClient =
            software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
                .builder()
                .dynamoDbClient(client)
                .build()
        val table = enhancedClient.table<TestEntity>(tableName)

        val entity = TestEntity(UUID.randomUUID().toString(), "Jane", 25)
        table.putItem(entity)

        table.getItem(entity.id).shouldNotBeNull()

        val deleted = table.deleteItem(entity.id)

        deleted.shouldNotBeNull()
        deleted.id shouldBeEqualTo entity.id
        table.getItem(entity.id).shouldBeNull()

        // cleanup
        client.deleteTable { it.tableName(tableName) }
    }

    @Test
    fun `findAll should return all items`() = runSuspendIO {
        val tableName = "sync-test-4-${UUID.randomUUID()}"

        // 테이블 생성
        client.createTable { builder ->
            builder.tableName(tableName)
            builder.attributeDefinitions(
                AttributeDefinition
                    .builder()
                    .attributeName("id")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
            )
            builder.keySchema(
                KeySchemaElement
                    .builder()
                    .attributeName("id")
                    .keyType(KeyType.HASH)
                    .build(),
            )
            builder.provisionedThroughput(
                ProvisionedThroughput
                    .builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build(),
            )
        }

        val enhancedClient =
            software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
                .builder()
                .dynamoDbClient(client)
                .build()
        val table = enhancedClient.table<TestEntity>(tableName)

        val entities =
            (1..5).map {
                TestEntity(UUID.randomUUID().toString(), "User-$it", 20 + it)
            }
        entities.forEach { table.putItem(it) }

        val results = table.findAll()

        results.size shouldBeEqualTo entities.size
        results.map { it.id }.toSet() shouldBeEqualTo entities.map { it.id }.toSet()

        // cleanup
        client.deleteTable { it.tableName(tableName) }
    }

    @Test
    fun `toList should convert PageIterable to List`() = runSuspendIO {
        val tableName = "sync-test-5-${UUID.randomUUID()}"

        // 테이블 생성
        client.createTable { builder ->
            builder.tableName(tableName)
            builder.attributeDefinitions(
                AttributeDefinition
                    .builder()
                    .attributeName("id")
                    .attributeType(ScalarAttributeType.S)
                    .build(),
            )
            builder.keySchema(
                KeySchemaElement
                    .builder()
                    .attributeName("id")
                    .keyType(KeyType.HASH)
                    .build(),
            )
            builder.provisionedThroughput(
                ProvisionedThroughput
                    .builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build(),
            )
        }

        val enhancedClient =
            software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
                .builder()
                .dynamoDbClient(client)
                .build()
        val table = enhancedClient.table<TestEntity>(tableName)

        val entities =
            (1..3).map {
                TestEntity(UUID.randomUUID().toString(), "User-$it", 20 + it)
            }
        entities.forEach { table.putItem(it) }

        val results = table.scan().toList()

        results.size shouldBeEqualTo entities.size

        // cleanup
        client.deleteTable { it.tableName(tableName) }
    }
}
