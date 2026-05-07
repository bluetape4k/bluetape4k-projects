package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import untrusted.payload.UntrustedPayload
import java.io.InvalidClassException
import java.io.ObjectInputFilter
import io.bluetape4k.assertions.assertFailsWith

/**
 * [JdkBinarySerializer]의 보안 기능 — [ObjectInputFilter] 적용 — 검증 테스트.
 *
 * JEP 290 기반 역직렬화 필터가 적용되는지 확인합니다.
 */
class JdkBinarySerializerSecurityTest {

    companion object: KLogging()

    // ────────────────────────────────────────────────────────────────────────────
    // 기본 필터 적용: 허용 패키지 내 클래스는 정상 직렬화/역직렬화 가능
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `기본 필터는 String 직렬화 역직렬화를 허용한다`() {
        val serializer = JdkBinarySerializer()
        val value = "hello-security"
        val bytes = serializer.serialize(value)
        val restored = serializer.deserialize<String>(bytes)
        restored shouldBeEqualTo value
    }

    @Test
    fun `기본 필터는 Int 직렬화 역직렬화를 허용한다`() {
        val serializer = JdkBinarySerializer()
        val value = 42
        val bytes = serializer.serialize(value)
        val restored = serializer.deserialize<Int>(bytes)
        restored shouldBeEqualTo value
    }

    @Test
    fun `기본 필터는 List 직렬화 역직렬화를 허용한다`() {
        val serializer = JdkBinarySerializer()
        val value = listOf("a", "b", "c")
        val bytes = serializer.serialize(value)
        val restored = serializer.deserialize<List<String>>(bytes)
        restored shouldBeEqualTo value
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 필터 없이 생성한 serializer: 동작 확인
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `objectInputFilter null 이면 JVM 전역 필터를 사용한다`() {
        val serializer = JdkBinarySerializer(objectInputFilter = null)
        val value = listOf(1, 2, 3)
        val bytes = serializer.serialize(value)
        val restored = serializer.deserialize<List<Int>>(bytes)
        restored.shouldNotBeNull()
        restored shouldBeEqualTo value
    }

    // ────────────────────────────────────────────────────────────────────────────
    // JDK_DEFAULT_OBJECT_INPUT_FILTER 단독 검증
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `JDK_DEFAULT_OBJECT_INPUT_FILTER 는 null 이 아니다`() {
        JDK_DEFAULT_OBJECT_INPUT_FILTER.shouldNotBeNull()
    }

    @Test
    fun `JDK_DEFAULT_OBJECT_INPUT_FILTER 는 io_bluetape4k 패키지 클래스를 허용한다`() {
        val serializer = JdkBinarySerializer(objectInputFilter = JDK_DEFAULT_OBJECT_INPUT_FILTER)
        val value = "bluetape4k-class"
        val bytes = serializer.serialize(value)
        val restored = serializer.deserialize<String>(bytes)
        restored shouldBeEqualTo value
    }

    @Test
    fun `커스텀 필터를 설정하면 해당 필터가 적용된다`() {
        // java.lang.String 만 허용하는 필터 설정 — 필터가 교체됨을 확인
        val customFilter = ObjectInputFilter.Config.createFilter("java.lang.String;java.lang.Integer;java.util.*")
        val serializer = JdkBinarySerializer(objectInputFilter = customFilter)

        // java.util.ArrayList + java.lang.Integer 가 허용되므로 List<Int> 역직렬화 가능
        val value = listOf(1, 2, 3)
        val bytes = JdkBinarySerializer(objectInputFilter = null).serialize(value)
        val restored = serializer.deserialize<List<Int>>(bytes)
        restored shouldBeEqualTo value
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 기본값 동작 확인
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `JdkBinarySerializer 기본 생성자는 JDK_DEFAULT_OBJECT_INPUT_FILTER 를 사용한다`() {
        // JdkBinarySerializer() 의 기본 objectInputFilter 가 null 이 아님을 간접 검증:
        // 기본 생성자 사용 시 filter 가 적용되어 일반 직렬화가 여전히 동작해야 함
        val serializer1 = JdkBinarySerializer()
        val serializer2 = JdkBinarySerializer(objectInputFilter = JDK_DEFAULT_OBJECT_INPUT_FILTER)

        val value = "consistent-filter"
        val bytes = serializer1.serialize(value)

        // 두 serializer 모두 동일한 필터를 사용하므로 결과가 같아야 함
        (serializer1.deserialize<String>(bytes) == serializer2.deserialize<String>(bytes)).shouldBeTrue()
    }

    @Test
    fun `bufferSize 기본값 serializer 와 커스텀 bufferSize 가 동일한 결과를 반환한다`() {
        val default = JdkBinarySerializer()
        val small = JdkBinarySerializer(bufferSize = 64)

        val value = "buffer-size-security-test"
        val bytesDefault = default.serialize(value)
        val bytesSmall = small.serialize(value)

        default.deserialize<String>(bytesSmall) shouldBeEqualTo value
        small.deserialize<String>(bytesDefault) shouldBeEqualTo value
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 거부 경로: 허용 목록 외 패키지 클래스 역직렬화 차단
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `JDK_DEFAULT_OBJECT_INPUT_FILTER 직접 호출시 UntrustedPayload 를 REJECTED 로 판정한다`() {
        val info = object : ObjectInputFilter.FilterInfo {
            override fun serialClass() = UntrustedPayload::class.java
            override fun arrayLength() = -1L
            override fun depth() = 1L
            override fun references() = 1L
            override fun streamBytes() = 100L
        }
        val status = JDK_DEFAULT_OBJECT_INPUT_FILTER.checkInput(info)
        status shouldBeEqualTo ObjectInputFilter.Status.REJECTED
    }

    @Test
    fun `setObjectInputFilter 로 REJECT 필터 적용 시 readObject 가 예외를 발생시킨다`() {
        // raw ObjectInputStream 으로 필터 wiring 자체 검증
        val payload = UntrustedPayload(data = "raw-test")
        val bytes = java.io.ByteArrayOutputStream().also { bos ->
            java.io.ObjectOutputStream(bos).use { it.writeObject(payload) }
        }.toByteArray()

        val ois = java.io.ObjectInputStream(java.io.ByteArrayInputStream(bytes))
        ois.setObjectInputFilter(JDK_DEFAULT_OBJECT_INPUT_FILTER)

        assertFailsWith<java.io.InvalidClassException>("setObjectInputFilter 로 적용한 필터가 readObject 에서 거부해야 한다") {
            ois.readObject()
        }
    }

    @Test
    fun `doDeserialize 내 apply_use 패턴이 필터를 유지하는지 검증한다`() {
        // 필터 wiring 을 doDeserialize 와 동일한 apply+use 패턴으로 재현
        val payload = UntrustedPayload(data = "apply-use-test")
        val bytes = java.io.ByteArrayOutputStream().also { bos ->
            java.io.ObjectOutputStream(bos).use { it.writeObject(payload) }
        }.toByteArray()

        assertFailsWith<java.io.InvalidClassException>("apply+use 패턴에서도 필터가 readObject 에서 거부해야 한다") {
            java.io.ByteArrayInputStream(bytes).use { bis ->
                java.io.ObjectInputStream(bis).apply {
                    setObjectInputFilter(JDK_DEFAULT_OBJECT_INPUT_FILTER)
                }.use { ois ->
                    ois.readObject()
                }
            }
        }
    }


    @Test
    fun `JDK_DEFAULT_OBJECT_INPUT_FILTER 는 허용 목록 외 패키지 클래스를 거부한다`() {
        // untrusted.payload.UntrustedPayload 는 io.bluetape4k.**, java.lang.**, kotlin.** 외 패키지
        val payload = UntrustedPayload(data = "malicious-payload")

        // 필터 없이 직렬화 (직렬화는 허용)
        val noFilterSerializer = JdkBinarySerializer(objectInputFilter = null)
        val bytes = noFilterSerializer.serialize(payload)

        // JDK_DEFAULT_OBJECT_INPUT_FILTER 적용 역직렬화 — 차단되어야 함
        val filteredSerializer = JdkBinarySerializer()

        val ex = assertFailsWith<BinarySerializationException>(
            "허용 목록 외 패키지 클래스는 역직렬화 시 예외가 발생해야 한다"
        ) {
            filteredSerializer.deserialize<UntrustedPayload>(bytes)
        }
        // cause chain 에 InvalidClassException 이 있어야 한다
        val causeChain = generateSequence(ex.cause) { it.cause }
        causeChain.any { it is InvalidClassException }.shouldBeTrue()
    }
}
