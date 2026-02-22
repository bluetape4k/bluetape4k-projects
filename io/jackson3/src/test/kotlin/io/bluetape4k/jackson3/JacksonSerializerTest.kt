package io.bluetape4k.jackson3

import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize
import io.bluetape4k.json.deserializeFromString
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class JacksonSerializerTest: AbstractJsonSerializerTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    override val serializer: JsonSerializer = JacksonSerializer()

    @Test
    fun `null 객체 직렬화 시 빈 바이트 배열 반환`() {
        val bytes = serializer.serialize(null)
        bytes.shouldBeEmpty()
    }

    @Test
    fun `null 바이트 배열 역직렬화 시 null 반환`() {
        val result = serializer.deserialize<User>(null as ByteArray?)
        result.shouldBeNull()
    }

    @Test
    fun `null 문자열 역직렬화 시 null 반환`() {
        val result = serializer.deserializeFromString<User>(null)
        result.shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `문자열 직렬화 및 역직렬화`(@RandomValue expected: User) {
        val jsonText = serializer.serializeAsString(expected)
        jsonText.shouldNotBeEmpty()

        val actual = serializer.deserializeFromString<User>(jsonText)
        actual.shouldNotBeNull() shouldBeEqualTo expected
    }

    @Test
    fun `잘못된 JSON 입력 역직렬화 시 예외를 던진다`() {
        assertFailsWith<JsonSerializationException> {
            serializer.deserialize<User>("{not-json".toByteArray())
        }
        assertFailsWith<JsonSerializationException> {
            serializer.deserializeFromString<User>("{not-json")
        }
    }

}
