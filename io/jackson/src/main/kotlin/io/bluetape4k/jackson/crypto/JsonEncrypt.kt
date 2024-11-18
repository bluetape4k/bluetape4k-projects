package io.bluetape4k.jackson.crypto

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.bluetape4k.crypto.encrypt.AES
import io.bluetape4k.crypto.encrypt.Encryptor
import kotlin.reflect.KClass

/**
 * Json Property 를 암호화하여 JSON 으로 만들고, 객체로 변환 시에 복호화 하는 기능을 제공합니다.
 *
 * ```
 * data class User(
 *     val username: String,
 *     @field:JsonEncrypt
 *     val password: String
 * )
 *
 * val mapper = ObjectMapperProvider.defaultObjectMapper
 * val user = User("debop", "debop@1968")
 * val encryptedText = mapper.writeAsString(user)
 *
 * encryptedText:
 * {
 *   "username" : "debop",
 *   "password" : "N1E79rV_n0d0eaZPMArKEwz4BxYEVP41ixR0T7SnF7k\r\n"
 * }
 *
 * @property encryptor
 * @constructor Create empty Json encrypt
 */
@JacksonAnnotationsInside
@JsonSerialize(using = JsonEncryptSerializer::class)
@JsonDeserialize(using = JsonEncryptDeserializer::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class JsonEncrypt(
    val encryptor: KClass<out Encryptor> = AES::class,
)
