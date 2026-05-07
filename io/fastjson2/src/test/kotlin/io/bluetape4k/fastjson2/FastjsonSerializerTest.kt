package io.bluetape4k.fastjson2

import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.fastjson2.model.newUser
import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

/**
 * [FastjsonSerializer]의 기본 동작을 검증하는 테스트 클래스입니다.
 *
 * [AbstractJsonSerializerTest]에서 상속받은 공통 테스트 외에
 * null 입력, 빈 바이트 배열, 문자열 직렬화/역직렬화를 추가 검증합니다.
 */
class FastjsonSerializerTest: AbstractJsonSerializerTest() {

    companion object: KLogging()

    override val serializer: FastjsonSerializer = FastjsonSerializer()

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
    fun `빈 바이트 배열 역직렬화 시 null 반환`() {
        val result = serializer.deserialize<User>(byteArrayOf())
        result.shouldBeNull()
    }

    @Test
    fun `null 직렬화 후 역직렬화 시 null 반환`() {
        val bytes = serializer.serialize(null)
        bytes.shouldBeEmpty()
        val result = serializer.deserialize(bytes, User::class.java)
        result.shouldBeNull()
    }

    @Test
    fun `null 문자열 역직렬화 시 null 반환`() {
        val result = serializer.deserializeFromString<User>(null)
        result.shouldBeNull()
    }

    @Test
    fun `빈 문자열 역직렬화 시 null 반환`() {
        val result = serializer.deserializeFromString<User>("")
        result.shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `문자열 직렬화 및 역직렬화`() {
        val user = newUser()
        val jsonText = serializer.serializeAsString(user)
        jsonText.shouldNotBeEmpty()

        val restored = serializer.deserializeFromString<User>(jsonText)
        restored.shouldNotBeNull() shouldBeEqualTo user
    }

    @Test
    fun `null 객체 문자열 직렬화 시 빈 문자열 반환`() {
        val jsonText = serializer.serializeAsString(null)
        jsonText.shouldBeEmpty()
    }

    @Test
    fun `Default 싱글턴 인스턴스 동작 확인`() {
        val user = newUser()
        val bytes = FastjsonSerializer.Default.serialize(user)
        bytes.shouldNotBeEmpty()

        val restored = FastjsonSerializer.Default.deserialize<User>(bytes)
        restored.shouldNotBeNull() shouldBeEqualTo user
    }

    @Test
    fun `잘못된 JSON 입력 역직렬화 시 예외를 던진다`() {
        assertFailsWith<JsonSerializationException> {
            serializer.deserialize<User>(byteArrayOf(1, 2, 3, 4))
        }
        assertFailsWith<JsonSerializationException> {
            serializer.deserializeFromString<User>("{not-json")
        }
    }

    @Test
    fun `Class 오버로드 - null 문자열 역직렬화 시 null 반환`() {
        val result = serializer.deserializeFromString(null, User::class.java)
        result.shouldBeNull()
    }

    @Test
    fun `Class 오버로드 - 빈 문자열 역직렬화 시 null 반환`() {
        val result = serializer.deserializeFromString("", User::class.java)
        result.shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Class 오버로드 - 문자열 직렬화 역직렬화 왕복`() {
        val user = newUser()
        val json = serializer.serializeAsString(user)
        json.shouldNotBeEmpty()

        val restored = serializer.deserializeFromString(json, User::class.java)
        restored.shouldNotBeNull() shouldBeEqualTo user
    }

    @Test
    fun `Class 오버로드 - 잘못된 JSON 역직렬화 시 예외를 던진다`() {
        assertFailsWith<JsonSerializationException> {
            serializer.deserializeFromString("{not-json", User::class.java)
        }
    }
}
