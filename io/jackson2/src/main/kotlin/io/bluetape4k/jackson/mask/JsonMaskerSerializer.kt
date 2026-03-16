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
 * [JsonMasker] 애너테이션이 붙은 문자열 필드를 마스킹하는 serializer입니다.
 *
 * ## 동작/계약
 * - 필드의 [JsonMasker] 값을 키로 serializer 인스턴스를 캐시합니다.
 * - 애너테이션이 있으면 실제 값 대신 마스킹 문자열을 기록합니다.
 * - 애너테이션이 없으면 원본 값을 그대로 기록합니다.
 *
 * ```kotlin
 * val serializer = JsonMaskerSerializer()
 * // @JsonMasker("__masked__") 필드는 "__masked__" 문자열로 직렬화됨
 * ```
 *
 * @property annotation 필드에 선언된 JsonMasker 애너테이션
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

    /** 필드 애너테이션에 맞는 serializer 인스턴스를 선택합니다. */
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

    /** 마스킹 규칙에 따라 문자열 값을 기록합니다. */
    override fun serialize(value: Any?, gen: JsonGenerator, provider: SerializerProvider?) {
        when {
            annotation != null -> gen.writeString(annotation.value)
            else -> gen.writeString(value.toString())
        }
    }
}
