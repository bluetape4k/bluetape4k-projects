package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ReturnValue
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class PutItemTest {

    companion object : KLogging()

    @Test
    fun `putItemRequestOf AttributeValue 항목으로 요청을 생성한다`() {
        val item = mapOf(
            "id" to AttributeValue.S("u1"),
            "name" to AttributeValue.S("Alice")
        )
        val req = putItemRequestOf("users", item)

        req.tableName shouldBeEqualTo "users"
        req.item.shouldNotBeNull()
        req.item!!.size shouldBeEqualTo 2
        req.item!!["id"] shouldBeEqualTo AttributeValue.S("u1")
    }

    @Test
    fun `putItemRequestOf Any 항목으로 요청을 생성한다`() {
        val item = mapOf("id" to "u2", "score" to 100)
        val req = putItemRequestOf("users", item)

        req.tableName shouldBeEqualTo "users"
        req.item!!["id"] shouldBeEqualTo AttributeValue.S("u2")
        req.item!!["score"] shouldBeEqualTo AttributeValue.N("100")
    }

    @Test
    fun `putItemRequestOf는 returnValues를 설정할 수 있다`() {
        val item = mapOf("id" to AttributeValue.S("u3"))
        val req = putItemRequestOf("users", item, returnValues = ReturnValue.AllOld)

        req.returnValues shouldBeEqualTo ReturnValue.AllOld
    }

    @Test
    fun `putItemRequestOf는 빈 tableName을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            putItemRequestOf("", mapOf("id" to AttributeValue.S("u1")))
        }
    }

    @Test
    fun `putItemRequestOf는 빈 item을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            putItemRequestOf("users", emptyMap<String, AttributeValue>())
        }
    }

    @Test
    fun `putItemRequestOf는 builder 블록으로 conditionExpression을 설정할 수 있다`() {
        val item = mapOf("id" to AttributeValue.S("u4"))
        val req = putItemRequestOf("users", item) {
            conditionExpression = "attribute_not_exists(id)"
        }

        req.conditionExpression shouldBeEqualTo "attribute_not_exists(id)"
    }
}
