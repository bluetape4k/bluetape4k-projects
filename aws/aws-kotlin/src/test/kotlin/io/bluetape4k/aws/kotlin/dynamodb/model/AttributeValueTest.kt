package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class AttributeValueTest {

    companion object : KLogging()

    @Test
    fun `null값은 AttributeValue Null로 변환된다`() {
        val av = null.toAttributeValue()
        av shouldBeInstanceOf AttributeValue.Null::class
        (av as AttributeValue.Null).value shouldBeEqualTo true
    }

    @Test
    fun `String은 AttributeValue S로 변환된다`() {
        val av = "hello".toAttributeValue()
        av shouldBeInstanceOf AttributeValue.S::class
        (av as AttributeValue.S).value shouldBeEqualTo "hello"
    }

    @Test
    fun `Int는 AttributeValue N으로 변환된다`() {
        val av = 42.toAttributeValue()
        av shouldBeInstanceOf AttributeValue.N::class
        (av as AttributeValue.N).value shouldBeEqualTo "42"
    }

    @Test
    fun `Long은 AttributeValue N으로 변환된다`() {
        val av = 123456789L.toAttributeValue()
        av shouldBeInstanceOf AttributeValue.N::class
        (av as AttributeValue.N).value shouldBeEqualTo "123456789"
    }

    @Test
    fun `Double은 AttributeValue N으로 변환된다`() {
        val av = 3.14.toAttributeValue()
        av shouldBeInstanceOf AttributeValue.N::class
    }

    @Test
    fun `Boolean은 AttributeValue Bool로 변환된다`() {
        val avTrue = true.toAttributeValue()
        avTrue shouldBeInstanceOf AttributeValue.Bool::class
        (avTrue as AttributeValue.Bool).value shouldBeEqualTo true

        val avFalse = false.toAttributeValue()
        (avFalse as AttributeValue.Bool).value shouldBeEqualTo false
    }

    @Test
    fun `ByteArray는 AttributeValue B로 변환된다`() {
        val bytes = byteArrayOf(1, 2, 3)
        val av = bytes.toAttributeValue()
        av shouldBeInstanceOf AttributeValue.B::class
    }

    @Test
    fun `ByteBuffer는 AttributeValue B로 변환된다`() {
        val buf = ByteBuffer.wrap(byteArrayOf(1, 2, 3))
        val av = buf.toAttributeValue()
        av shouldBeInstanceOf AttributeValue.B::class
    }

    @Test
    fun `List는 AttributeValue L로 변환된다`() {
        val list = listOf("a", "b", "c")
        val av = list.toAttributeValue()
        av shouldBeInstanceOf AttributeValue.L::class
        (av as AttributeValue.L).value.size shouldBeEqualTo 3
    }

    @Test
    fun `Map은 AttributeValue M으로 변환된다`() {
        val map = mapOf("name" to "Alice", "age" to 30)
        val av = map.toAttributeValue()
        av shouldBeInstanceOf AttributeValue.M::class
        val m = (av as AttributeValue.M).value
        m.shouldNotBeNull()
        m["name"] shouldBeEqualTo AttributeValue.S("Alice")
        m["age"] shouldBeEqualTo AttributeValue.N("30")
    }

    @Test
    fun `toAttributeValueList는 Iterable 요소를 AttributeValue 목록으로 변환한다`() {
        val items = listOf("x", "y", "z")
        val avList = items.toAttributeValueList()

        avList.size shouldBeEqualTo 3
        avList[0] shouldBeEqualTo AttributeValue.S("x")
    }

    @Test
    fun `toAttributeValueMap는 Map을 String-AttributeValue 맵으로 변환한다`() {
        val map = mapOf("id" to "u1", "score" to 100)
        val avMap = map.toAttributeValueMap()

        avMap["id"] shouldBeEqualTo AttributeValue.S("u1")
        avMap["score"] shouldBeEqualTo AttributeValue.N("100")
    }

    @Test
    fun `AttributeValue 자신은 그대로 반환된다`() {
        val original = AttributeValue.S("test")
        val result = original.toAttributeValue()
        result shouldBeEqualTo original
    }
}
