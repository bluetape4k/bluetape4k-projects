package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class BatchGetItemTest {

    companion object : KLogging()

    @Test
    fun `batchGetItemRequestOf는 requestItems로 요청을 생성한다`() {
        val keys = listOf(mapOf("id" to AttributeValue.S("u1")))
        val keysAndAttrs = KeysAndAttributes { this.keys = keys }
        val req = batchGetItemRequestOf(mapOf("users" to keysAndAttrs))

        req.requestItems.shouldNotBeNull()
        req.requestItems!!.size shouldBeEqualTo 1
        req.requestItems!!.containsKey("users") shouldBeEqualTo true
    }

    @Test
    fun `batchGetItemRequestOf는 여러 테이블 요청을 지원한다`() {
        val userKeys = KeysAndAttributes {
            keys = listOf(mapOf("id" to AttributeValue.S("u1")))
        }
        val orderKeys = KeysAndAttributes {
            keys = listOf(mapOf("orderId" to AttributeValue.S("o1")))
        }
        val req = batchGetItemRequestOf(mapOf("users" to userKeys, "orders" to orderKeys))

        req.requestItems!!.size shouldBeEqualTo 2
    }

    @Test
    fun `batchGetItemRequestOf는 빈 requestItems를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            batchGetItemRequestOf(emptyMap())
        }
    }

    @Test
    fun `batchGetItemRequestOf는 builder 블록으로 추가 설정이 가능하다`() {
        val keys = listOf(mapOf("id" to AttributeValue.S("u1")))
        val keysAndAttrs = KeysAndAttributes { this.keys = keys }
        val req = batchGetItemRequestOf(mapOf("users" to keysAndAttrs)) {
            returnConsumedCapacity = aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity.Total
        }

        req.shouldNotBeNull()
    }
}
