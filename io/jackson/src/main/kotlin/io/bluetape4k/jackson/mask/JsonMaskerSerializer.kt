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

    /** [JsonMasker] 어노테이션에 따라 적절한 마스킹 직렬화기 인스턴스를 반환합니다. */
    override fun createContextual(
        prov: SerializerProvider?, property: BeanProperty?,
    ): JsonSerializer<*> {
        val annotation = property?.getAnnotation(JsonMasker::class.java)

        return when (annotation) {
            null -> defaultSerializer
            else -> serializers.getOrPut(annotation.value) {
                JsonMaskerSerializer(annotation)
                    .apply {
                        log.debug { "Create JsonMaskerSerializer ... ${annotation.value}" }
                    }
            }
        }
    }

    /** [JsonMasker] 어노테이션이 적용된 값을 마스킹 문자열로 대체하여 JSON에 씁니다. */
    override fun serialize(value: Any?, gen: JsonGenerator, provider: SerializerProvider?) {
        when {
            annotation != null -> gen.writeString(annotation.value)
            else -> gen.writeRawValue(value.toString())
        }
    }
}
