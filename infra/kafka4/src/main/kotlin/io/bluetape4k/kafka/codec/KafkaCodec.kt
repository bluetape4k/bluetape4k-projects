package io.bluetape4k.kafka.codec

import io.bluetape4k.LibraryName
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.support.classIsPresent
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import kotlinx.coroutines.CancellationException
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serializer
import java.io.Closeable
import java.nio.ByteBuffer

/**
 * Kafka 의 [Serializer], [Deserializer] 기능을 한번에 제공하는 Codec 입니다.
 *
 * ```kotlin
 * val codec = KafkaCodecs.String
 * val serialized = codec.serialize("my-topic", "hello")
 * val deserialized = codec.deserialize("my-topic", serialized)
 * // deserialized == "hello"
 * ```
 *
 * @param T 메시지 Value 의 수형
 */
interface KafkaCodec<T>:
    Serializer<T>,
    Deserializer<T>,
    Closeable {
    override fun configure(
        configs: MutableMap<String, *>?,
        isKey: Boolean,
    ) {
        // Nothing to do
    }

    override fun serialize(
        topic: String?,
        data: T?,
    ): ByteArray = serialize(topic, null, data)

    override fun deserialize(
        topic: String?,
        data: ByteArray?,
    ): T? = deserialize(topic, null, data)

    override fun close() {
        // Nothing to do
    }
}

/**
 * Opt-in Kafka codec contract for caller-owned [ByteBuffer] input and output.
 *
 * Standard Kafka [Serializer] and [Deserializer] calls remain [ByteArray]-based. These methods avoid an additional
 * Kafka-layer array conversion when the backing codec already supports buffers; the concrete serializer still
 * determines whether its implementation is optimized or an allocating compatibility fallback.
 *
 * Output advances the target position by the returned byte count on success. Input reads the source's current
 * remaining range while preserving caller state. Buffers remain caller-owned and must not be mutated concurrently.
 */
interface BufferAwareKafkaCodec<T>: KafkaCodec<T> {
    fun serializeTo(topic: String?, data: T & Any, target: ByteBuffer): Int =
        serializeTo(topic, null, data, target)

    fun serializeTo(
        topic: String?,
        headers: Headers?,
        data: T & Any,
        target: ByteBuffer,
    ): Int

    fun deserializeFrom(topic: String?, source: ByteBuffer): T? =
        deserializeFrom(topic, null, source)

    fun deserializeFrom(topic: String?, headers: Headers?, source: ByteBuffer): T?
}

/**
 * [KafkaCodec]의 기본 추상 구현체입니다.
 *
 * 직렬화 시 헤더에 Value 타입 정보를 기록하고, 역직렬화 시 이를 참조합니다.
 *
 * ```kotlin
 * val codec = JacksonKafkaCodec()
 * val serialized = codec.serialize("my-topic", mapOf("key" to "value"))
 * val deserialized = codec.deserialize("my-topic", serialized)
 * // deserialized is a Map with key -> value
 * ```
 *
 * **보안 경고**: 이 코덱은 Kafka 헤더의 `bluetape4k.kafka.codec.value.type` 값을 기반으로
 * 클래스를 동적으로 로드합니다. 신뢰할 수 없는 Kafka 브로커 또는 외부 네트워크에서
 * 수신된 메시지에 이 코덱을 사용하면 임의 클래스 로딩(RCE) 취약점이 발생할 수 있습니다.
 * 보안이 필요한 환경에서는 반드시 [allowedTypePackages]에 허용된 패키지를 지정하십시오.
 */
abstract class AbstractKafkaCodec<T>: KafkaCodec<T> {
    companion object: KLogging() {
        const val VALUE_TYPE_KEY = "$LibraryName.kafka.codec.value.type"

        /**
         * Sentinel value for [allowedTypePackages] that bypasses all package checks.
         *
         * Use only in fully trusted, internal deployments where all Kafka producers are
         * controlled. Assigning this constant re-enables the pre-1.8.0 allow-all behavior.
         *
         * ```kotlin
         * class LegacyJacksonCodec : JacksonKafkaCodec() {
         *     override val allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE
         * }
         * ```
         */
        @JvmField
        val ALLOW_ALL_TYPES_UNSAFE: Set<String> = setOf("*")

        @JvmStatic
        fun defaultCodec(): KafkaCodec<Any?> = JacksonKafkaCodec()
    }

    /**
     * Package prefix allowlist for types loaded from the [VALUE_TYPE_KEY] header.
     *
     * Each entry is matched against the incoming class name as follows:
     * - **Exact match**: `clazzName == entry` — allows a specific fully-qualified class name.
     * - **Package prefix**: `clazzName.startsWith("$entry.")` — allows all classes under that package.
     *
     * Semantics by value:
     * - **Empty set (default)**: deny all — no class is loaded from the header.
     *   Safe default for untrusted or shared Kafka topics.
     * - **Non-empty set**: allow only classes whose FQN exactly equals or starts with `"<entry>."`.
     * - **[ALLOW_ALL_TYPES_UNSAFE]**: bypass all checks (pre-1.8.0 allow-all behavior, unsafe).
     *
     * ```kotlin
     * // Safe: allow all DTOs under a specific package
     * val codec = JacksonKafkaCodec(allowedTypePackages = setOf("com.example.dto"))
     *
     * // Also safe: allow only one specific class
     * val singleClass = JacksonKafkaCodec(allowedTypePackages = setOf("com.example.dto.OrderEvent"))
     *
     * // Unsafe legacy mode (opt-in only)
     * val legacy = JacksonKafkaCodec(allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE)
     * ```
     */
    open val allowedTypePackages: Set<String> = emptySet()

    /**
     * 직렬화 시 `bluetape4k.kafka.codec.value.type` 헤더에 Java FQN을 기록할지 여부.
     *
     * - `true` (기본값): 매 레코드에 타입 헤더를 기록합니다. 다형성 역직렬화 시 필요하지만
     *   대역폭·저장 오버헤드가 발생하며 외부 공격자가 헤더를 조작할 수 있는 attack surface를 넓힙니다.
     * - `false`: 헤더를 생략합니다. 역직렬화 시 타입 헤더를 읽지 않는 바이너리 코덱
     *   ([ForyKafkaCodec], [KryoKafkaCodec])에서 권장합니다.
     *   [JacksonKafkaCodec]은 역직렬화 시 헤더에서 타입을 로드하므로 `false` 설정 시
     *   `doDeserialize`도 함께 오버라이드해야 합니다.
     *
     * ```kotlin
     * // Fory/Kryo 기반 코덱은 헤더 없이 안전하게 작동합니다
     * class NoHeaderForyCodec: ForyKafkaCodec() {
     *     override val writeValueTypeHeader = false
     * }
     * ```
     */
    open val writeValueTypeHeader: Boolean = true

    protected abstract fun doSerialize(
        topic: String?,
        headers: Headers?,
        graph: T,
    ): ByteArray

    protected abstract fun doDeserialize(
        topic: String?,
        headers: Headers?,
        bytes: ByteArray,
    ): T?

    override fun serialize(
        topic: String?,
        headers: Headers?,
        data: T?,
    ): ByteArray? =
        data?.run {
            if (writeValueTypeHeader) setValueType(headers, data.javaClass)
            doSerialize(topic, headers, this)
        }

    /**
     * 헤더의 [VALUE_TYPE_KEY] 를 참조해 역직렬화한다.
     *
     * **Poison-pill 정책**:
     * - `Exception` 발생 시 WARN 로그를 남기고 `null` 을 반환해 컨슈머 루프 진행을 막지 않는다.
     * - 영구 손실을 막으려면 Spring-Kafka 의 `ErrorHandlingDeserializer` + `DeadLetterPublishingRecoverer` 를 함께 사용하라.
     *
     * **흡수하지 않는 예외**:
     * - [CancellationException] — 항상 재던진다 (코루틴 취소 신호 보존).
     * - [Error] (OOM/StackOverflow 등) — JVM 상태가 손상된 상황에서 swallow 하면 위험하므로 그대로 전파한다.
     *
     * ```kotlin
     * val codec = KafkaCodecs.Jackson
     * val payload = codec.deserialize("orders", headers, bytes) // null 이면 poison pill — 메트릭/DLQ 로 라우팅 권장
     * ```
     */
    override fun deserialize(
        topic: String?,
        headers: Headers?,
        data: ByteArray?,
    ): T? =
        try {
            data?.run { doDeserialize(topic, headers, this) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn(e) {
                "Fail to deserialize data. topic=$topic, headerKeys=${headers?.map { it.key() }}, dataSize=${data?.size}. Returning null (poison pill skipped)."
            }
            null
        }

    protected fun setValueType(
        headers: Headers?,
        valueType: Class<T & Any>,
    ) {
        headers?.add(VALUE_TYPE_KEY, valueType.name.toUtf8Bytes())
    }

    protected fun getValueType(headers: Headers?): Class<*> {
        val clazzName =
            headers?.lastHeader(VALUE_TYPE_KEY)?.value()?.toUtf8String()
                ?: return Any::class.java

        // "*" sentinel bypasses all checks (ALLOW_ALL_TYPES_UNSAFE)
        if (!allowedTypePackages.contains("*")) {
            if (allowedTypePackages.isEmpty() ||
                allowedTypePackages.none { clazzName == it || clazzName.startsWith("$it.") }
            ) {
                log.warn {
                    "[SECURITY] Rejected class '$clazzName' from Kafka header — not in allowedTypePackages=$allowedTypePackages. " +
                    "If intentional, add the package to allowedTypePackages or use ALLOW_ALL_TYPES_UNSAFE (unsafe)."
                }
                throw IllegalArgumentException(
                    "Class '$clazzName' is not in allowedTypePackages=$allowedTypePackages. " +
                    "Add the package to allowedTypePackages, or set allowedTypePackages = AbstractKafkaCodec.ALLOW_ALL_TYPES_UNSAFE " +
                    "to allow all types (unsafe)."
                )
            }
        }

        if (!classIsPresent(clazzName)) {
            throw IllegalArgumentException("클래스를 클래스패스에서 찾을 수 없습니다. clazzName=$clazzName")
        }

        return try {
            Class.forName(clazzName, false, Thread.currentThread().contextClassLoader)
        } catch (e: Exception) {
            throw IllegalArgumentException("클래스 로딩 실패. clazzName=$clazzName", e)
        }
    }
}
