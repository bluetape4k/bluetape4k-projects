package io.bluetape4k.jackson.crypto

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.bluetape4k.crypto.encrypt.AES
import io.bluetape4k.crypto.encrypt.Encryptor
import kotlin.reflect.KClass

/**
 * 문자열 필드를 JSON 직렬화 시 암호화하고 역직렬화 시 복호화하도록 지정하는 애너테이션입니다.
 *
 * ## 동작/계약
 * - 직렬화는 [JsonEncryptSerializer], 역직렬화는 [JsonEncryptDeserializer]가 처리합니다.
 * - [encryptor]로 지정한 타입의 인스턴스를 [JsonEncryptors]에서 조회해 사용합니다.
 * - 필드 값 자체는 객체 메모리에서 변경하지 않고 JSON 표현만 암복호화합니다.
 *
 * ```kotlin
 * data class User(
 *     val username: String,
 *     @field:JsonEncrypt val password: String,
 * )
 * // password 필드는 암호문 문자열로 직렬화됨
 * ```
 *
 * @property encryptor 사용할 Encryptor 구현 타입
 */
@JacksonAnnotationsInside
@JsonSerialize(using = JsonEncryptSerializer::class)
@JsonDeserialize(using = JsonEncryptDeserializer::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class JsonEncrypt(
    val encryptor: KClass<out Encryptor> = AES::class,
)
