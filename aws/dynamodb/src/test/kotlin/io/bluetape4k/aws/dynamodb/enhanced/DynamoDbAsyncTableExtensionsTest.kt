package io.bluetape4k.aws.dynamodb.enhanced

import io.bluetape4k.aws.dynamodb.AbstractDynamodbTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
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
import java.io.Serializable
import java.util.*

class DynamoDbAsyncTableExtensionsTest: AbstractDynamodbTest() {

    companion object: KLoggingChannel()

    @DynamoDbBean
    data class TestEntity(
        @get:DynamoDbPartitionKey
        var id: String = "",
        var name: String = "",
        var age: Int = 0,
    ): Serializable


    @Test
    fun `getItem by partition key should return item`() = runSuspendIO {
        val tableName = "async-test-${UUID.randomUUID()}"
        val table = enhancedAsyncClient.table<TestEntity>(tableName)

        // 테이블 생성
        asyncClient
            .createTable { builder ->
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
            }.await()

        // 테이블 활성화 대기
        delay(500)

        val entity = TestEntity(UUID.randomUUID().toString(), "John", 30)
        table.putItem(entity)
        delay(100)

        val result = table.getItem(entity.id)

        result.shouldNotBeNull()
        result.id shouldBeEqualTo entity.id
        result.name shouldBeEqualTo entity.name
        result.age shouldBeEqualTo entity.age

        // cleanup
        asyncClient.deleteTable { it.tableName(tableName) }.await()
    }

    @Test
    fun `getItem with non-existent key should return null`() = runSuspendIO {
        val tableName = "async-test-2-${UUID.randomUUID()}"
        val table = enhancedAsyncClient.table<TestEntity>(tableName)

        // 테이블 생성
        asyncClient
            .createTable { builder ->
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
            }.await()

        delay(500)

        val result = table.getItem("non-existent-id")
        result.shouldBeNull()

        // cleanup
        asyncClient.deleteTable { it.tableName(tableName) }.await()
    }

    @Test
    fun `deleteItem should remove item`() = runSuspendIO {
        val tableName = "async-test-3-${UUID.randomUUID()}"
        val table = enhancedAsyncClient.table<TestEntity>(tableName)

        // 테이블 생성
        asyncClient
            .createTable { builder ->
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
            }.await()

        delay(500)

        val entity = TestEntity(UUID.randomUUID().toString(), "Jane", 25)
        table.putItem(entity)
        delay(100)

        val beforeDelete = table.getItem(entity.id)
        beforeDelete.shouldNotBeNull()

        val deleted = table.deleteItem(entity.id)

        deleted.shouldNotBeNull()
        deleted.id shouldBeEqualTo entity.id

        delay(100)
        val afterDelete = table.getItem(entity.id)
        afterDelete.shouldBeNull()

        // cleanup
        asyncClient.deleteTable { it.tableName(tableName) }.await()
    }

    @Test
    fun `exists should return true for existing item`() = runSuspendIO {
        val tableName = "async-test-5-${UUID.randomUUID()}"
        val table = enhancedAsyncClient.table<TestEntity>(tableName)

        // 테이블 생성
        asyncClient
            .createTable { builder ->
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
            }.await()

        delay(500)

        val entity = TestEntity(UUID.randomUUID().toString(), "Test", 30)
        table.putItem(entity)
        delay(100)

        val exists = table.exists(entity.id)
        exists shouldBeEqualTo true

        // cleanup
        asyncClient.deleteTable { it.tableName(tableName) }.await()
    }

    @Test
    fun `exists should return false for non-existing item`() = runSuspendIO {
        val tableName = "async-test-6-${UUID.randomUUID()}"
        val table = enhancedAsyncClient.table<TestEntity>(tableName)

        // 테이블 생성
        asyncClient
            .createTable { builder ->
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
            }.await()

        delay(500)

        val exists = table.exists("non-existent")
        exists shouldBeEqualTo false

        // cleanup
        asyncClient.deleteTable { it.tableName(tableName) }.await()
    }
}
