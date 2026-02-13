package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ReturnValue
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DynamoDbModelSupportTest {

    @Test
    fun `createTableRequestOf는 tableName과 class를 설정한다`() {
        val request = createTableRequestOf(tableName = "test-table")

        request.tableName shouldBeEqualTo "test-table"
    }

    @Test
    fun `putItemRequestOf any-map은 attribute value로 변환한다`() {
        val request = putItemRequestOf(
            tableName = "test-table",
            item = mapOf("id" to "1", "age" to 10),
            returnValues = ReturnValue.AllOld,
        )

        request.tableName shouldBeEqualTo "test-table"
        request.returnValues shouldBeEqualTo ReturnValue.AllOld
        request.item!!["id"] shouldBeEqualTo AttributeValue.S("1")
        request.item!!["age"] shouldBeEqualTo AttributeValue.N("10")
    }

    @Test
    fun `scanRequestOf any-map은 exclusiveStartKey를 변환한다`() {
        val request = scanRequestOf(
            tableName = "test-table",
            exclusiveStartKey = mapOf("id" to "1"),
        )

        request.exclusiveStartKey?.get("id") shouldBeEqualTo AttributeValue.S("1")
    }

    @Test
    fun `queryRequestOf any-map은 exclusiveStartKey를 변환한다`() {
        val request = queryRequestOf(
            tableName = "test-table",
            exclusiveStartKey = mapOf("id" to "1"),
        )

        request.exclusiveStartKey?.get("id") shouldBeEqualTo AttributeValue.S("1")
    }

    @Test
    fun `updateOf any-map은 key를 변환한다`() {
        val request = updateOf(
            tableName = "test-table",
            key = mapOf("id" to "1"),
            updateExpression = "SET #name = :name",
            expressionAttributeValues = mapOf(":name" to AttributeValue.S("debop")),
            expressionAttributeNames = mapOf("#name" to "name"),
        )

        request.key["id"] shouldBeEqualTo AttributeValue.S("1")
        request.updateExpression shouldBeEqualTo "SET #name = :name"
    }

    @Test
    fun `batchWriteItemRequestOf는 requestItems를 설정한다`() {
        val writeRequest = WriteRequest {
            putRequest { item = mapOf("id" to AttributeValue.S("1")) }
        }

        val request = batchWriteItemRequestOf(mapOf("test-table" to listOf(writeRequest)))

        request.requestItems!!["test-table"]?.size shouldBeEqualTo 1
    }

    @Test
    fun `putItemRequestOf는 빈 item을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            putItemRequestOf(tableName = "test-table", item = emptyMap<String, Any?>())
        }
    }

    @Test
    fun `batchWriteItemRequestOf는 빈 requestItems를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            batchWriteItemRequestOf(emptyMap())
        }
    }
}
