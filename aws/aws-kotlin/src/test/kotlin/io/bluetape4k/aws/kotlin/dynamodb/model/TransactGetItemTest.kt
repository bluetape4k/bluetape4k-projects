package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.Get
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class TransactGetItemTest {

    companion object : KLogging()

    @Test
    fun `transactGetItemOf Get 객체로 TransactGetItem을 생성한다`() {
        val get = Get {
            tableName = "users"
            key = mapOf("id" to AttributeValue.S("u1"))
        }
        val item = transactGetItemOf(get)

        item.get.shouldNotBeNull()
        item.get!!.tableName shouldBeEqualTo "users"
        item.get!!.key!!["id"] shouldBeEqualTo AttributeValue.S("u1")
    }

    @Test
    fun `transactGetItemOf tableName과 builder로 TransactGetItem을 생성한다`() {
        val item = transactGetItemOf("users", emptyMap()) {
            projectionExpression = "id, name"
        }

        item.get.shouldNotBeNull()
        item.get!!.tableName shouldBeEqualTo "users"
    }

    @Test
    fun `transactGetItemOf builder 블록으로 projectionExpression을 설정할 수 있다`() {
        val item = transactGetItemOf("users", emptyMap()) {
            projectionExpression = "id, name, email"
        }

        item.get.shouldNotBeNull()
        item.get!!.projectionExpression shouldBeEqualTo "id, name, email"
    }

    @Test
    fun `transactGetItemsRequestOf는 transactItems 목록으로 요청을 생성한다`() {
        val get = Get {
            tableName = "users"
            key = mapOf("id" to AttributeValue.S("u1"))
        }
        val transactItem = transactGetItemOf(get)
        val req = transactGetItemsRequestOf(listOf(transactItem))

        req.transactItems.shouldNotBeNull()
        req.transactItems!!.size shouldBeEqualTo 1
    }

    @Test
    fun `transactGetItemsRequestOf는 빈 transactItems를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            transactGetItemsRequestOf(emptyList())
        }
    }
}
