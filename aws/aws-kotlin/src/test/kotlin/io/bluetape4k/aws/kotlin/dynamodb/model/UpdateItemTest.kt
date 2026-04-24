package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class UpdateItemTest {

    companion object : KLogging()

    @Test
    fun `updateOf AttributeValue 키와 표현식으로 Update를 생성한다`() {
        val key = mapOf("id" to AttributeValue.S("u1"))
        val exprValues = mapOf(":name" to AttributeValue.S("Alice"))
        val update = updateOf(
            tableName = "users",
            key = key,
            updateExpression = "SET #n = :name",
            expressionAttributeValues = exprValues,
            expressionAttributeNames = mapOf("#n" to "name")
        )

        update.tableName shouldBeEqualTo "users"
        update.updateExpression shouldBeEqualTo "SET #n = :name"
        update.expressionAttributeValues.shouldNotBeNull()
        update.expressionAttributeValues!![":name"] shouldBeEqualTo AttributeValue.S("Alice")
    }

    @Test
    fun `updateOf는 conditionExpression을 설정할 수 있다`() {
        val key = mapOf("id" to AttributeValue.S("u2"))
        val exprValues = mapOf(":score" to AttributeValue.N("100"))
        val update = updateOf(
            tableName = "users",
            key = key,
            updateExpression = "SET score = :score",
            expressionAttributeValues = exprValues,
            conditionExpression = "attribute_exists(id)"
        )

        update.conditionExpression shouldBeEqualTo "attribute_exists(id)"
    }

    @Test
    fun `updateOf는 빈 tableName을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            updateOf(
                tableName = "",
                key = mapOf("id" to AttributeValue.S("u1")),
                updateExpression = "SET name = :name",
                expressionAttributeValues = mapOf(":name" to AttributeValue.S("Bob"))
            )
        }
    }

    @Test
    fun `updateOf는 빈 key를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            updateOf(
                tableName = "users",
                key = emptyMap(),
                updateExpression = "SET name = :name",
                expressionAttributeValues = mapOf(":name" to AttributeValue.S("Bob"))
            )
        }
    }

    @Test
    fun `updateOf는 빈 updateExpression을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            updateOf(
                tableName = "users",
                key = mapOf("id" to AttributeValue.S("u1")),
                updateExpression = "",
                expressionAttributeValues = emptyMap()
            )
        }
    }
}
