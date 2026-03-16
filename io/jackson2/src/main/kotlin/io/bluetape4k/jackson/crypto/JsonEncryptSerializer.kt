package io.bluetape4k.jackson.crypto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.safeLet
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * [JsonEncrypt] 애너테이션이 붙은 문자열 필드를 암호화해 직렬화하는 serializer입니다.
 *
 * ## 동작/계약
 * - 필드의 encryptor 타입별 serializer 인스턴스를 캐시합니다.
 * - 애너테이션과 값이 모두 있을 때만 암호화 문자열을 기록합니다.
 * - 애너테이션이 없거나 값이 null이면 원본 값을 그대로 기록합니다.
 *
 * ```kotlin
 * val serializer = JsonEncryptSerializer()
 * // @JsonEncrypt 필드는 암호문 문자열로 직렬화됨
 * ```
 *
 * @property annotation 필드에 선언된 JsonEncrypt 애너테이션
 *
 * @see JsonEncrypt
 */
class JsonEncryptSerializer(
    private val annotation: JsonEncrypt? = null,
): StdSerializer<String>(String::class.java), ContextualSerializer {

    companion object: KLogging() {
        private val defaultSerializer = JsonEncryptSerializer()
        private val serializers = ConcurrentHashMap<KClass<out Encryptor>, JsonEncryptSerializer>()
    }

    /** 필드 애너테이션에 맞는 serializer 인스턴스를 선택합니다. */
    override fun createContextual(prov: SerializerProvider?, property: BeanProperty?): JsonSerializer<*> {
        val annotation = property?.getAnnotation(JsonEncrypt::class.java)

        return when (annotation) {
            null -> defaultSerializer
            else -> serializers.getOrPut(annotation.encryptor) {
                JsonEncryptSerializer(annotation).apply {
                    log.debug { "create JsonEncryptSerializer ... ${annotation.encryptor}" }
                }
            }
        }
    }

    /** 문자열 값을 암호화 규칙에 따라 기록합니다. */
    override fun serialize(value: String?, gen: JsonGenerator, provider: SerializerProvider?) {
        safeLet(annotation, value) { ann, v ->
            val encryptor = JsonEncryptors.getEncryptor(ann.encryptor)
            val encryptedText = encryptor.encrypt(v)
            gen.writeString(encryptedText)
        } ?: gen.writeString(value)
    }
}
