package io.bluetape4k.aws.kotlin.dynamodb

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class DynamoItemMapperTest {

    data class TestItem(
        val id: String,
        val name: String,
        val age: Int,
    )

    private val testMapper = DynamoItemMapper<TestItem> { item ->
        mapOf(
            "id" to AttributeValue.S(item.id),
            "name" to AttributeValue.S(item.name),
            "age" to AttributeValue.N(item.age.toString()),
        )
    }

    private val keyMapper = DynamoItemMapper<TestItem> { item ->
        mapOf("id" to AttributeValue.S(item.id))
    }

    @Test
    fun `DynamoItemMapper SAM 인터페이스로 매핑 생성`() {
        val item = TestItem("1", "Alice", 30)
        val result = testMapper.mapToDynamoItem(item)

        result["id"] shouldBeEqualTo AttributeValue.S("1")
        result["name"] shouldBeEqualTo AttributeValue.S("Alice")
        result["age"] shouldBeEqualTo AttributeValue.N("30")
    }

    @Test
    fun `buildWritePutRequests는 모든 아이템을 WriteRequest로 변환한다`() {
        val items = listOf(
            TestItem("1", "Alice", 30),
            TestItem("2", "Bob", 25),
        )

        val writeRequests = items.buildWritePutRequests(testMapper)

        writeRequests shouldHaveSize 2
        writeRequests[0].putRequest.shouldNotBeNull()
        writeRequests[0].putRequest!!.item["id"] shouldBeEqualTo AttributeValue.S("1")
        writeRequests[1].putRequest!!.item["name"] shouldBeEqualTo AttributeValue.S("Bob")
    }

    @Test
    fun `buildWritePutRequests 빈 목록은 빈 결과 반환`() {
        val writeRequests = emptyList<TestItem>().buildWritePutRequests(testMapper)
        writeRequests shouldHaveSize 0
    }

    @Test
    fun `buildWriteDeleteRequests는 키를 기반으로 삭제 요청 생성`() {
        val items = listOf(
            TestItem("1", "Alice", 30),
            TestItem("2", "Bob", 25),
        )

        val writeRequests = items.buildWriteDeleteRequests(keyMapper)

        writeRequests shouldHaveSize 2
        writeRequests[0].deleteRequest.shouldNotBeNull()
        writeRequests[0].deleteRequest!!.key["id"] shouldBeEqualTo AttributeValue.S("1")
        writeRequests[1].deleteRequest!!.key["id"] shouldBeEqualTo AttributeValue.S("2")
    }

    @Test
    fun `buildWriteDeleteRequests 빈 목록은 빈 결과 반환`() {
        val writeRequests = emptyList<TestItem>().buildWriteDeleteRequests(keyMapper)
        writeRequests shouldHaveSize 0
    }
}
