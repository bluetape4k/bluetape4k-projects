package io.bluetape4k.jackson3.mask

import io.bluetape4k.logging.KLogging
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.introspect.Annotated
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector
import java.util.concurrent.ConcurrentHashMap

/**
 * [JsonMasker] 애너테이션을 읽어 마스킹 serializer를 선택하는 인트로스펙터입니다.
 *
 * ## 동작/계약
 * - 애너테이션이 존재할 때만 커스텀 serializer를 반환합니다.
 * - 마스킹 문자열 값별 serializer 인스턴스를 캐시합니다.
 *
 * ```kotlin
 * val introspector = JsonMaskerAnnotationIntrospector()
 * // @JsonMasker 필드에 대해 serializer를 선택함
 * ```
 *
 * @see [JsonMasker]
 * @see [JsonMaskerModule]
 */
class JsonMaskerAnnotationIntrospector: JacksonAnnotationIntrospector() {

    companion object: KLogging() {
        private val ANNOTATION_TYPE = JsonMasker::class.java
        private val serializers = ConcurrentHashMap<String, JsonMaskerSerializer>()
    }

    /** 직렬화기 선택 규칙을 반환합니다. */
    override fun findSerializer(config: MapperConfig<*>?, a: Annotated?): Any? {
        val jsonMasker = _findAnnotation(a, ANNOTATION_TYPE)
        return jsonMasker?.let { masker ->
            serializers.computeIfAbsent(masker.value) {
                JsonMaskerSerializer(masker)
            }
        }
    }
}

/** "Interospector" 오타를 수정한 [JsonMaskerAnnotationIntrospector]의 하위 호환 별칭입니다. */
@Deprecated(
    "오타가 수정된 JsonMaskerAnnotationIntrospector 를 사용하세요.",
    ReplaceWith("JsonMaskerAnnotationIntrospector")
)
typealias JsonMaskerAnnotationInterospector = JsonMaskerAnnotationIntrospector
