package io.bluetape4k.fastjson2

import io.bluetape4k.fastjson2.model.User
import io.bluetape4k.fastjson2.model.newUser
import io.bluetape4k.json.JsonSerializationException
import io.bluetape4k.json.JsonSerializer
import io.bluetape4k.json.deserialize
import io.bluetape4k.json.deserializeFromString
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * [FastjsonSerializer]의 기본 동작을 검증하는 테스트 클래스입니다.
 *
 * [AbstractJsonSerializerTest]에서 상속받은 공통 테스트 외에
 * null 입력, 빈 바이트 배열, 문자열 직렬화/역직렬화를 추가 검증합니다.
 */
class FastjsonSerializerTest: AbstractJsonSerializerTest() {

    companion object: KLogging()

    override val serializer: JsonSerializer = FastjsonSerializer()

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
}
