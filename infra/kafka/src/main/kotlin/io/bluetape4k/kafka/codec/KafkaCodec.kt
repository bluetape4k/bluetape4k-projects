package io.bluetape4k.kafka.codec

import io.bluetape4k.LibraryName
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.warn
import io.bluetape4k.support.classIsPresent
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
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
 * 수신된 메시지에 이 코덱을 사용하면 임의 클래스 로딩 취약점이 발생할 수 있습니다.
 * 반드시 신뢰할 수 있는 내부 Kafka 브로커 환경에서만 사용하십시오.
 */
abstract class AbstractKafkaCodec<T>: KafkaCodec<T> {
    companion object: KLogging() {
        const val VALUE_TYPE_KEY = "$LibraryName.kafka.codec.value.type"

        @JvmStatic
        fun defaultCodec(): KafkaCodec<Any?> = JacksonKafkaCodec()
    }

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

    override fun deserialize(
        topic: String?,
        headers: Headers?,
        data: ByteArray?,
    ): T? =
        try {
            data?.run { doDeserialize(topic, headers, this) }
        } catch (e: Throwable) {
            log.warn(e) { "Fail to deserialize data. topic=$topic, headerKeys=${headers?.map { it.key() }}, data=$data. Returning null (poison pill skipped)." }
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

        return try {
            when {
                classIsPresent(clazzName) -> {
                    Class.forName(clazzName, false, Thread.currentThread().contextClassLoader)
                }
                else -> {
                    Any::class.java
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Fail to load value type. clazzName=$clazzName" }
            Any::class.java
        }
    }
}
