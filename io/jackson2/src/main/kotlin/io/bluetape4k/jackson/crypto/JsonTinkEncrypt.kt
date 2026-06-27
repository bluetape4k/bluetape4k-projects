package io.bluetape4k.jackson.crypto

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

/**
 * Marks a string field for Google Tink encryption during JSON serialization and
 * decryption during JSON deserialization.
 *
 * ## Contract
 * - Serialization is handled by [JsonTinkEncryptSerializer], and deserialization
 *   is handled by [JsonTinkEncryptDeserializer].
 * - The selected [algorithm] resolves to a [TinkEncryptors] singleton.
 * - Singleton encryptors use process-local in-memory keysets; do not use this
 *   annotation for durable encrypted database columns or searchable indexes.
 * - The in-memory object value is unchanged; only the JSON representation is
 *   encrypted or decrypted.
 *
 * ```kotlin
 * data class User(
 *     val username: String,
 *     @get:JsonTinkEncrypt val password: String,
 *     @get:JsonTinkEncrypt(algorithm = TinkEncryptAlgorithm.DETERMINISTIC_AES256_SIV) val mobile: String,
 * )
 * // password is serialized as an AES256-GCM ciphertext string.
 * // mobile is serialized as a deterministic AES256-SIV ciphertext string for this JVM keyset.
 * ```
 *
 * @property algorithm Tink encryption algorithm. Defaults to [TinkEncryptAlgorithm.AES256_GCM].
 */
@JacksonAnnotationsInside
@JsonSerialize(using = JsonTinkEncryptSerializer::class)
@JsonDeserialize(using = JsonTinkEncryptDeserializer::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
annotation class JsonTinkEncrypt(
    val algorithm: TinkEncryptAlgorithm = TinkEncryptAlgorithm.AES256_GCM,
)
