package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class DynamoDbDeleteTest {

    companion object : KLogging()

    @Test
    fun `deleteOf AttributeValue 키로 Delete를 생성한다`() {
        val key = mapOf("id" to AttributeValue.S("u1"))
        val del = deleteOf("users", key)

        del.tableName shouldBeEqualTo "users"
        del.key.shouldNotBeNull()
        del.key!!["id"] shouldBeEqualTo AttributeValue.S("u1")
    }

    @Test
    fun `deleteOf Any 키로 Delete를 생성한다`() {
        val del = deleteOf("users", mapOf("id" to "u1"))

        del.tableName shouldBeEqualTo "users"
        del.key.shouldNotBeNull()
        del.key!!["id"] shouldBeEqualTo AttributeValue.S("u1")
    }

    @Test
    fun `deleteOf는 빈 tableName을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            deleteOf("", mapOf("id" to AttributeValue.S("u1")))
        }
    }

    @Test
    fun `deleteRequestOf AttributeValue 키로 DeleteRequest를 생성한다`() {
        val key = mapOf("id" to AttributeValue.S("u2"))
        val req = deleteRequestOf(key)

        req.key.shouldNotBeNull()
        req.key!!["id"] shouldBeEqualTo AttributeValue.S("u2")
    }

    @Test
    fun `deleteRequestOf는 빈 key를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            deleteRequestOf(emptyMap<String, AttributeValue>())
        }
    }

    @Test
    fun `deleteOf builder 블록으로 conditionExpression을 설정할 수 있다`() {
        val del = deleteOf("users", mapOf("id" to AttributeValue.S("u3"))) {
            conditionExpression = "attribute_exists(id)"
        }

        del.conditionExpression shouldBeEqualTo "attribute_exists(id)"
    }
}
