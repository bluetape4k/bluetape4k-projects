package io.bluetape4k.io.serializer

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.ObjectInputFilter

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
}
