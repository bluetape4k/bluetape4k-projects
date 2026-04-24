package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class QueryRequestTest {

    companion object : KLogging()

    @Test
    fun `queryRequestOf AttributeValue 오버로드로 tableName으로 요청을 생성한다`() {
        val req = queryRequestOf(
            tableName = "users",
            exclusiveStartKey = null as Map<String, AttributeValue>?
        )

        req.tableName shouldBeEqualTo "users"
    }

    @Test
    fun `queryRequestOf는 attributesToGet을 설정할 수 있다`() {
        val req = queryRequestOf(
            tableName = "users",
            attributesToGet = listOf("id", "email"),
            exclusiveStartKey = null as Map<String, AttributeValue>?
        )

        req.attributesToGet.shouldNotBeNull()
        req.attributesToGet!! shouldContain "id"
        req.attributesToGet!! shouldContain "email"
    }

    @Test
    fun `queryRequestOf는 builder 블록으로 keyConditionExpression을 설정할 수 있다`() {
        val exprValues = mapOf(":id" to AttributeValue.S("u1"))
        val req = queryRequestOf(
            tableName = "users",
            exclusiveStartKey = null as Map<String, AttributeValue>?
        ) {
            keyConditionExpression = "id = :id"
            expressionAttributeValues = exprValues
        }

        req.keyConditionExpression shouldBeEqualTo "id = :id"
        req.expressionAttributeValues.shouldNotBeNull()
        req.expressionAttributeValues!![":id"] shouldBeEqualTo AttributeValue.S("u1")
    }

    @Test
    fun `queryRequestOf AttributeValue 시작 키로 페이지네이션을 설정할 수 있다`() {
        val startKey = mapOf("id" to AttributeValue.S("u10"))
        val req = queryRequestOf(
            tableName = "users",
            exclusiveStartKey = startKey
        )

        req.exclusiveStartKey.shouldNotBeNull()
        req.exclusiveStartKey!!["id"] shouldBeEqualTo AttributeValue.S("u10")
    }

    @Test
    fun `queryRequestOf Any 키 오버로드로 exclusiveStartKey를 설정할 수 있다`() {
        val startKey: Map<String, Any?> = mapOf("id" to "u10")
        val req = queryRequestOf(
            tableName = "users",
            exclusiveStartKey = startKey
        )

        req.exclusiveStartKey.shouldNotBeNull()
        req.exclusiveStartKey!!["id"] shouldBeEqualTo AttributeValue.S("u10")
    }

    @Test
    fun `queryRequestOf AttributeValue 오버로드는 빈 tableName을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            queryRequestOf(
                tableName = "",
                exclusiveStartKey = null as Map<String, AttributeValue>?
            )
        }
    }
}
