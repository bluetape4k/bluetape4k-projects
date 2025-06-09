package io.bluetape4k.jackson.mask

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import java.util.concurrent.ConcurrentHashMap

/**
 * [JsonMasker] 가 적용된 필드의 정보를 masking 하는 Jackson 용 Serializer 입니다.
 *
 * @property annotation [JsonMasker] annotation or null
 *
 * @see [JsonMasker]
 */
class JsonMaskerSerializer(
    private val annotation: JsonMasker? = null,
): StdSerializer<Any>(Any::class.java), ContextualSerializer {

    companion object: KLogging() {
        private val defaultSerializer = JsonMaskerSerializer()
        private val serializers: MutableMap<String, JsonMaskerSerializer> = ConcurrentHashMap()
    }

    override fun createContextual(
        prov: SerializerProvider?, property: BeanProperty?,
    ): JsonSerializer<*> {
        val annotation = property?.getAnnotation(JsonMasker::class.java)

        return when (annotation) {
            null -> defaultSerializer
            else -> serializers.computeIfAbsent(annotation.value) {
                JsonMaskerSerializer(annotation)
                    .apply {
                        log.debug { "Create JsonMaskerSerializer ... ${annotation.value}" }
                    }
            }
        }
    }

    override fun serialize(value: Any?, gen: JsonGenerator, provider: SerializerProvider?) {
        when {
            annotation != null -> gen.writeString(annotation.value)
            else -> gen.writeRawValue(value.toString())
        }
    }
}
