package io.bluetape4k.jackson3.crypto

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import io.bluetape4k.crypto.encrypt.AES
import io.bluetape4k.crypto.encrypt.Encryptor
import kotlin.reflect.KClass

/**
 * 문자열 필드를 JSON 직렬화 시 암호화하고 역직렬화 시 복호화하도록 지정하는 애너테이션입니다.
 *
 * ## 동작/계약
 * - [JsonEncryptModule] 등록 시 애너테이션 인트로스펙터가 serializer/deserializer를 선택합니다.
 * - [encryptor] 타입으로 지정한 Encryptor 인스턴스를 사용합니다.
 * - 객체 내부 값은 변경하지 않고 JSON 표현만 암복호화합니다.
 *
 * ```kotlin
 * data class User(
 *     val username: String,
 *     @field:JsonEncrypt val password: String,
 * )
 * // password 필드는 암호문 문자열로 직렬화됨
 *
 * @property encryptor 사용할 Encryptor 구현 타입
 */
@JacksonAnnotationsInside
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY)
annotation class JsonEncrypt(
    val encryptor: KClass<out Encryptor> = AES::class,
)
