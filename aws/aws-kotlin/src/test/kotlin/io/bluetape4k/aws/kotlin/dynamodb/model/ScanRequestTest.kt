package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ScanRequestTest {

    companion object : KLogging()

    @Test
    fun `scanRequestOf AttributeValue 오버로드로 tableName으로 요청을 생성한다`() {
        val req = scanRequestOf(
            tableName = "users",
            exclusiveStartKey = null as Map<String, AttributeValue>?
        )

        req.tableName shouldBeEqualTo "users"
    }

    @Test
    fun `scanRequestOf는 attributesToGet을 설정할 수 있다`() {
        val req = scanRequestOf(
            tableName = "users",
            attributesToGet = listOf("id", "name"),
            exclusiveStartKey = null as Map<String, AttributeValue>?
        )

        req.attributesToGet.shouldNotBeNull()
        req.attributesToGet!! shouldContain "id"
        req.attributesToGet!! shouldContain "name"
    }

    @Test
    fun `scanRequestOf는 indexName을 설정할 수 있다`() {
        val req = scanRequestOf(
            tableName = "users",
            indexName = "status-index",
            exclusiveStartKey = null as Map<String, AttributeValue>?
        )

        req.indexName shouldBeEqualTo "status-index"
    }

    @Test
    fun `scanRequestOf는 exclusiveStartKey(AttributeValue)를 설정할 수 있다`() {
        val startKey = mapOf("id" to AttributeValue.S("u10"))
        val req = scanRequestOf(
            tableName = "users",
            exclusiveStartKey = startKey
        )

        req.exclusiveStartKey.shouldNotBeNull()
        req.exclusiveStartKey!!["id"] shouldBeEqualTo AttributeValue.S("u10")
    }

    @Test
    fun `scanRequestOf Any 키 오버로드로 exclusiveStartKey를 설정할 수 있다`() {
        val startKey: Map<String, Any?> = mapOf("id" to "u10")
        val req = scanRequestOf(
            tableName = "users",
            exclusiveStartKey = startKey
        )

        req.exclusiveStartKey.shouldNotBeNull()
        req.exclusiveStartKey!!["id"] shouldBeEqualTo AttributeValue.S("u10")
    }

    @Test
    fun `scanRequestOf AttributeValue 오버로드는 빈 tableName을 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            scanRequestOf(
                tableName = "",
                exclusiveStartKey = null as Map<String, AttributeValue>?
            )
        }
    }
}
