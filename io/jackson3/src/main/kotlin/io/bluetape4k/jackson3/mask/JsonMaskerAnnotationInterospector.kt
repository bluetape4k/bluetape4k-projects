package io.bluetape4k.jackson3.mask

import io.bluetape4k.logging.KLogging
import tools.jackson.databind.cfg.MapperConfig
import tools.jackson.databind.introspect.Annotated
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector
import java.util.concurrent.ConcurrentHashMap

/**
 *  [JsonMasker] annotation 이 적용된 속성의 값을 지정된 masking 된 문자열로 직렬화 합니다.
 *
 *  @see [JsonMasker]
 *  @see [JsonMaskerModule]
 */
class JsonMaskerAnnotationInterospector: JacksonAnnotationIntrospector() {

    companion object: KLogging() {
        private val ANNOTATION_TYPE = JsonMasker::class.java
        private val serializers = ConcurrentHashMap<String, JsonMaskerSerializer>()
    }

    /**
     * [JsonMasker] 어노테이션이 적용된 필드에 대해 마스킹 직렬화기를 반환합니다.
     */
    override fun findSerializer(config: MapperConfig<*>?, a: Annotated?): Any? {
        val jsonMasker = _findAnnotation(a, ANNOTATION_TYPE)
        return jsonMasker?.let {
            serializers.getOrPut(jsonMasker.value) {
                JsonMaskerSerializer(jsonMasker)
            }
        }
    }
}
