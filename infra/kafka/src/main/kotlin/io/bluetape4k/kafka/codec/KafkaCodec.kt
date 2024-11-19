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
 * @param T 메시지 Value 의 수형
 */
interface KafkaCodec<T>: Serializer<T>, Deserializer<T>, Closeable {

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
        // Nothing to do
    }

    override fun serialize(topic: String?, data: T?): ByteArray {
        return serialize(topic, null, data)
    }

    override fun deserialize(topic: String?, data: ByteArray?): T? {
        return deserialize(topic, null, data)
    }

    override fun close() {
        // Nothing to do 
    }
}

abstract class AbstractKafkaCodec<T>: KafkaCodec<T> {

    companion object: KLogging() {
        const val VALUE_TYPE_KEY = "$LibraryName.kafka.codec.value.type"

        @JvmStatic
        fun defaultCodec(): KafkaCodec<Any?> = JacksonKafkaCodec()
    }

    protected abstract fun doSerialize(topic: String?, headers: Headers?, graph: T): ByteArray
    protected abstract fun doDeserialize(topic: String?, headers: Headers?, bytes: ByteArray): T?

    override fun serialize(topic: String?, headers: Headers?, data: T?): ByteArray? {
        return data?.run {
            setValueType(headers, data.javaClass)
            doSerialize(topic, headers, this)
        }
    }

    override fun deserialize(topic: String?, headers: Headers?, data: ByteArray?): T? {
        return try {
            data?.run { doDeserialize(topic, headers, this) }
        } catch (e: Throwable) {
            log.warn(e) { "Fail to deserialize data. topic=$topic, headers=$headers, data=$data" }
            null
        }
    }

    protected fun setValueType(headers: Headers?, valueType: Class<T & Any>) {
        headers?.add(VALUE_TYPE_KEY, valueType.name.toUtf8Bytes())
    }

    protected fun getValueType(headers: Headers?): Class<*> {
        val clazzName = headers?.lastHeader(VALUE_TYPE_KEY)?.value()?.toUtf8String()
            ?: return Any::class.java

        return try {
            when {
                classIsPresent(clazzName) ->
                    Class.forName(clazzName, true, Thread.currentThread().contextClassLoader)

                else                      -> Any::class.java
            }
        } catch (e: Exception) {
            log.error(e) { " Fail to load value type. clazzName=$clazzName" }
            Any::class.java
        }
    }
}
