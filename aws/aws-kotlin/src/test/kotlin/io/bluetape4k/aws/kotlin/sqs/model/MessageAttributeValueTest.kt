package io.bluetape4k.aws.kotlin.sqs.model

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class MessageAttributeValueTest {

    companion object : KLogging()

    @Test
    fun `messageAttributeValueOf String으로 StringValue를 설정한다`() {
        val attr = messageAttributeValueOf("hello")

        attr.stringValue shouldBeEqualTo "hello"
    }

    @Test
    fun `messageAttributeValueOf null String을 허용한다`() {
        val attr = messageAttributeValueOf(null as String?)

        attr.shouldNotBeNull()
    }

    @Test
    fun `messageAttributeValueOf String 목록으로 StringListValues를 설정한다`() {
        val attr = messageAttributeValueOf(listOf("a", "b", "c"))

        attr.stringListValues.shouldNotBeNull()
        attr.stringListValues!! shouldBeEqualTo listOf("a", "b", "c")
    }

    @Test
    fun `messageAttributeValueOf ByteArray로 BinaryValue를 설정한다`() {
        val bytes = byteArrayOf(1, 2, 3)
        val attr = messageAttributeValueOf(bytes)

        attr.binaryValue.shouldNotBeNull()
    }

    @Test
    fun `messageAttributeValueOf ByteArray 목록으로 BinaryListValues를 설정한다`() {
        val values = listOf(byteArrayOf(1), byteArrayOf(2))
        val attr = messageAttributeValueOf(values)

        attr.binaryListValues.shouldNotBeNull()
        attr.binaryListValues!!.size shouldBeEqualTo 2
    }

    @Test
    fun `messageAttributeValueOf builder 블록으로 추가 설정이 가능하다`() {
        val attr = messageAttributeValueOf("test") {
            dataType = "String"
        }

        attr.dataType shouldBeEqualTo "String"
    }
}
