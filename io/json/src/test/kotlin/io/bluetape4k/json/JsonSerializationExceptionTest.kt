package io.bluetape4k.json

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * [JsonSerializationException]의 생성자, 메시지, 원인(cause), 직렬화 가능성을 검증합니다.
 */
class JsonSerializationExceptionTest {
    companion object : KLogging()

    @Test
    fun `기본 생성자로 예외 생성 시 메시지와 원인이 null`() {
        val ex = JsonSerializationException()
        ex.message.shouldBeNull()
        ex.cause.shouldBeNull()
    }

    @Test
    fun `메시지 생성자로 생성 시 메시지가 설정됨`() {
        val ex = JsonSerializationException("직렬화 실패")
        ex.message shouldBeEqualTo "직렬화 실패"
        ex.cause.shouldBeNull()
    }

    @Test
    fun `메시지와 원인 생성자로 생성 시 모두 설정됨`() {
        val cause = IllegalArgumentException("원인 예외")
        val ex = JsonSerializationException("직렬화 실패", cause)
        ex.message shouldBeEqualTo "직렬화 실패"
        ex.cause shouldBeEqualTo cause
    }

    @Test
    fun `원인만 있는 생성자로 생성 시 원인이 설정됨`() {
        val cause = RuntimeException("원본 오류")
        val ex = JsonSerializationException(cause)
        ex.cause shouldBeEqualTo cause
    }

    @Test
    fun `RuntimeException을 상속하는지 확인`() {
        val ex = JsonSerializationException("테스트")
        ex.shouldBeInstanceOf<RuntimeException>()
    }

    @Test
    fun `Serializable을 구현하는지 확인`() {
        val ex = JsonSerializationException("직렬화 테스트")
        ex.shouldBeInstanceOf<Serializable>()
    }

    @Test
    fun `Java 직렬화-역직렬화 후 메시지가 보존됨`() {
        val original = JsonSerializationException("직렬화 보존 테스트")

        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(original) }

        val bais = ByteArrayInputStream(baos.toByteArray())
        val restored = ObjectInputStream(bais).use { it.readObject() }

        restored.shouldNotBeNull()
        restored.shouldBeInstanceOf<JsonSerializationException>()
        (restored as JsonSerializationException).message shouldBeEqualTo original.message
    }

    @Test
    fun `하위 클래스로 확장 가능함`() {
        class CustomJsonException(
            message: String,
        ) : JsonSerializationException(message)

        val ex = CustomJsonException("커스텀 예외")
        ex.shouldBeInstanceOf<JsonSerializationException>()
        ex.message shouldBeEqualTo "커스텀 예외"
    }
}
