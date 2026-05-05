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

        @JvmStatic
        fun defaultCodec(): KafkaCodec<Any?> = JacksonKafkaCodec()
    }

    /**
     * 헤더에서 로드를 허용하는 클래스 패키지 접두사 목록.
     *
     * - **빈 집합(기본값)**: 모든 클래스 허용 (하위 호환, 신뢰 환경 전용)
     * - **비어 있지 않은 집합**: 나열된 패키지로 시작하는 클래스만 허용 (권장, 보안 환경)
     *
     * ```kotlin
     * class SecureJacksonCodec: JacksonKafkaCodec() {
     *     override val allowedTypePackages = setOf("com.example.dto", "io.bluetape4k.domain")
     * }
     * ```
     */
    open val allowedTypePackages: Set<String> = emptySet()

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
            setValueType(headers, data.javaClass)
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

        if (allowedTypePackages.isNotEmpty() &&
            allowedTypePackages.none { clazzName == it || clazzName.startsWith("$it.") }
        ) {
            throw IllegalArgumentException(
                "클래스 '$clazzName'은 허용된 패키지 목록에 없습니다. allowedTypePackages=$allowedTypePackages"
            )
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
