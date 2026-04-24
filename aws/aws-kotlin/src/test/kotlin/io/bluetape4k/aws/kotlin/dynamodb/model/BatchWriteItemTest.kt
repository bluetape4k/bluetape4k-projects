package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutRequest
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class BatchWriteItemTest {

    companion object : KLogging()

    @Test
    fun `batchWriteItemRequestOf는 requestItems로 요청을 생성한다`() {
        val putReq = PutRequest {
            item = mapOf("id" to AttributeValue.S("u1"))
        }
        val writeReq = WriteRequest { putRequest = putReq }
        val req = batchWriteItemRequestOf(mapOf("users" to listOf(writeReq)))

        req.requestItems.shouldNotBeNull()
        req.requestItems!!.size shouldBeEqualTo 1
        req.requestItems!!.containsKey("users") shouldBeEqualTo true
    }

    @Test
    fun `batchWriteItemRequestOf는 여러 테이블 요청을 지원한다`() {
        val userPut = WriteRequest { putRequest = PutRequest { item = mapOf("id" to AttributeValue.S("u1")) } }
        val orderPut = WriteRequest { putRequest = PutRequest { item = mapOf("orderId" to AttributeValue.S("o1")) } }

        val req = batchWriteItemRequestOf(
            mapOf(
                "users" to listOf(userPut),
                "orders" to listOf(orderPut)
            )
        )

        req.requestItems!!.size shouldBeEqualTo 2
    }

    @Test
    fun `batchWriteItemRequestOf는 빈 requestItems를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            batchWriteItemRequestOf(emptyMap())
        }
    }

    @Test
    fun `batchWriteItemRequestOf는 builder 블록으로 추가 설정이 가능하다`() {
        val writeReq = WriteRequest {
            putRequest = PutRequest { item = mapOf("id" to AttributeValue.S("u1")) }
        }
        val req = batchWriteItemRequestOf(mapOf("users" to listOf(writeReq))) {
            returnConsumedCapacity = aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity.Total
        }

        req.shouldNotBeNull()
        req.returnConsumedCapacity shouldBeEqualTo aws.sdk.kotlin.services.dynamodb.model.ReturnConsumedCapacity.Total
    }
}
