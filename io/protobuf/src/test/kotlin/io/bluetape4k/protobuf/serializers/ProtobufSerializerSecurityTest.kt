package io.bluetape4k.protobuf.serializers

import com.google.protobuf.Any
import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.protobuf.messages.TestMessage
import io.bluetape4k.protobuf.messages.testMessage
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * [ProtobufSerializer] 보안 기능 테스트.
 *
 * - 허용 목록 외 클래스 접두사는 [SecurityException] 을 발생시켜야 한다.
 * - 허용 목록 내 클래스는 정상 역직렬화된다.
 * - 커스텀 [allowedClassPrefixes] 가 반영된다.
 */
class ProtobufSerializerSecurityTest {

    companion object: KLogging()

    // ────────────────────────────────────────────────────────────────────────────
    // 정상 경로: 허용 목록 내 클래스
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `기본 허용 목록으로 io_bluetape4k 패키지 클래스를 역직렬화할 수 있다`() {
        val serializer = ProtobufSerializer()
        val message = testMessage {
            id = 42L
            name = "security-test"
        }
        val bytes = serializer.serialize(message)
        val restored = serializer.deserialize<TestMessage>(bytes)
        restored shouldBeEqualTo message
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 보안 차단: 허용 목록 외 클래스
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `typeUrl 이 허용 목록 외 클래스이면 BinarySerializationException 이 발생한다`() {
        // 직접 Any 메시지를 조작하여 허용 목록 외 typeUrl 을 갖는 바이트 배열 생성
        // com.malicious.EvilPayload 는 DEFAULT_ALLOWED_PREFIXES 에 없음
        val maliciousTypeUrl = "type.googleapis.com/com.malicious.EvilPayload"
        val craftedAny = Any.newBuilder()
            .setTypeUrl(maliciousTypeUrl)
            .setValue(com.google.protobuf.ByteString.copyFromUtf8("evil-data"))
            .build()
        val bytes = craftedAny.toByteArray()

        val serializer = ProtobufSerializer()

        // AbstractBinarySerializer.deserialize 가 SecurityException 을 BinarySerializationException 으로 감쌈
        val ex = assertFailsWith<BinarySerializationException>("허용 목록 외 클래스는 직렬화 예외를 발생시켜야 한다") {
            serializer.deserialize<kotlin.Any>(bytes)
        }
        // 원인이 SecurityException 이어야 한다
        val cause = generateSequence(ex.cause) { it.cause }
        cause.any { it is SecurityException }.shouldBeTrue()
    }

    @Test
    fun `typeUrl 이 허용 목록 외 패키지이면 SecurityException 메시지에 클래스명이 포함된다`() {
        val maliciousClass = "org.apache.commons.collections.functors.InvokerTransformer"
        val craftedAny = Any.newBuilder()
            .setTypeUrl("type.googleapis.com/$maliciousClass")
            .setValue(com.google.protobuf.ByteString.copyFromUtf8("payload"))
            .build()
        val bytes = craftedAny.toByteArray()

        val serializer = ProtobufSerializer()

        // AbstractBinarySerializer.deserialize 가 SecurityException 을 BinarySerializationException 으로 감쌈
        val ex = assertFailsWith<BinarySerializationException> {
            serializer.deserialize<kotlin.Any>(bytes)
        }
        // cause chain 에 SecurityException 이 있고, 그 메시지에 클래스명이 포함됨
        val secEx = generateSequence(ex.cause) { it.cause }.filterIsInstance<SecurityException>().firstOrNull()
        (secEx?.message?.contains(maliciousClass) == true).shouldBeTrue()
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 커스텀 allowedClassPrefixes
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `커스텀 allowedClassPrefixes 로 허용 목록을 좁히면 기본 허용 클래스도 차단된다`() {
        // io.bluetape4k. 를 제거하고 com.example.only. 만 허용
        val narrowSerializer = ProtobufSerializer(
            allowedClassPrefixes = setOf("com.example.only.")
        )
        val message = testMessage { id = 1L; name = "narrow-test" }
        val bytes = narrowSerializer.serialize(message)

        // io.bluetape4k.protobuf.messages.TestMessage 는 com.example.only. 로 시작하지 않음
        // AbstractBinarySerializer.deserialize 가 SecurityException 을 BinarySerializationException 으로 감쌈
        val ex = assertFailsWith<BinarySerializationException> {
            narrowSerializer.deserialize<TestMessage>(bytes)
        }
        val cause = generateSequence(ex.cause) { it.cause }
        cause.any { it is SecurityException }.shouldBeTrue()
    }

    @Test
    fun `커스텀 allowedClassPrefixes 에 패키지를 추가하면 해당 패키지가 허용된다`() {
        // io.bluetape4k. 와 함께 com.example. 추가
        val expandedSerializer = ProtobufSerializer(
            allowedClassPrefixes = setOf("io.bluetape4k.", "com.google.protobuf.", "com.example.")
        )
        // 기존 io.bluetape4k 클래스는 여전히 허용됨
        val message = testMessage { id = 99L; name = "expanded-test" }
        val bytes = expandedSerializer.serialize(message)
        val restored = expandedSerializer.deserialize<TestMessage>(bytes)
        restored shouldBeEqualTo message
    }

    // ────────────────────────────────────────────────────────────────────────────
    // DEFAULT_ALLOWED_PREFIXES 내용 검증
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `DEFAULT_ALLOWED_PREFIXES 는 io_bluetape4k 와 com_google_protobuf 를 포함한다`() {
        val prefixes = ProtobufSerializer.DEFAULT_ALLOWED_PREFIXES
        prefixes.contains("io.bluetape4k.").shouldBeTrue()
        prefixes.contains("com.google.protobuf.").shouldBeTrue()
    }
}
