package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.KeyType
import aws.sdk.kotlin.services.dynamodb.model.ReturnValue
import aws.sdk.kotlin.services.dynamodb.model.ScalarAttributeType
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
    fun `attributeDefinition 계열 함수는 타입을 설정한다`() {
        val stringAttr = "id".stringAttributeDefinition()
        val numberAttr = "age".numberAttributeDefinition()
        val binaryAttr = "payload".binaryAttributeDefinition()

        stringAttr.attributeName shouldBeEqualTo "id"
        stringAttr.attributeType shouldBeEqualTo ScalarAttributeType.S
        numberAttr.attributeType shouldBeEqualTo ScalarAttributeType.N
        binaryAttr.attributeType shouldBeEqualTo ScalarAttributeType.B
    }

    @Test
    fun `attributeDefinitionOf는 빈 이름을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            attributeDefinitionOf(" ", ScalarAttributeType.S)
        }
    }

    @Test
    fun `toAttributeValueMap은 키를 문자열로 변환한다`() {
        val attrMap = mapOf(100 to "value", true to 1).toAttributeValueMap()

        attrMap["100"] shouldBeEqualTo AttributeValue.S("value")
        attrMap["true"] shouldBeEqualTo AttributeValue.N("1")
    }

    @Test
    fun `getItemRequestOf any-map은 key를 AttributeValue로 변환한다`() {
        val request = getItemRequestOf(
            attributesToGet = listOf("id"),
            consistentRead = true,
            key = mapOf("id" to "a-1"),
        )

        request.attributesToGet shouldBeEqualTo listOf("id")
        request.consistentRead shouldBeEqualTo true
        request.key?.get("id") shouldBeEqualTo AttributeValue.S("a-1")
    }

    @Test
    fun `deleteTableRequestOf는 tableName을 설정하고 검증한다`() {
        val request = deleteTableRequestOf("test-table")
        request.tableName shouldBeEqualTo "test-table"

        assertFailsWith<IllegalArgumentException> {
            deleteTableRequestOf(" ")
        }
    }

    @Test
    fun `partitionKey와 sortKey는 올바른 key type을 설정한다`() {
        "pk".partitionKey().keyType shouldBeEqualTo KeyType.Hash
        "sk".sortKey().keyType shouldBeEqualTo KeyType.Range
    }

    @Test
    fun `writeRequestOf는 put과 delete를 각각 생성한다`() {
        val putRequest = putRequestOf(mapOf("id" to "1"))
        val putWriteRequest = writeRequestOf(putRequest)
        assertNotNull(putWriteRequest.putRequest)
        assertNull(putWriteRequest.deleteRequest)

        val deleteRequest = deleteRequestOf(mapOf("id" to "1"))
        val deleteWriteRequest = writeRequestOf(deleteRequest = deleteRequest)
        assertNotNull(deleteWriteRequest.deleteRequest)
        assertNull(deleteWriteRequest.putRequest)
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
