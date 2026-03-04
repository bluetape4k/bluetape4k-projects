package io.bluetape4k.jackson3.crypto

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside

/**
 * 문자열 필드를 JSON 직렬화 시 Google Tink로 암호화하고 역직렬화 시 복호화하도록 지정하는 애너테이션입니다.
 *
 * ## 동작/계약
 * - [JsonTinkEncryptModule] 등록 시 [JsonTinkEncryptAnnotationIntrospector]가 serializer/deserializer를 선택합니다.
 * - [algorithm]으로 지정한 [TinkEncryptAlgorithm]의 [TinkEncryptors] 인스턴스를 사용합니다.
 * - 객체 내부 값은 변경하지 않고 JSON 표현만 암복호화합니다.
 *
 * ```kotlin
 * data class User(
 *     val username: String,
 *     @get:JsonTinkEncrypt val password: String,
 *     @get:JsonTinkEncrypt(algorithm = TinkEncryptAlgorithm.DETERMINISTIC_AES256_SIV) val mobile: String,
 * )
 * // password 필드는 AES256-GCM 암호문 문자열로 직렬화됨
 * // mobile 필드는 결정적 AES256-SIV 암호문 문자열로 직렬화됨
 * ```
 *
 * @property algorithm 사용할 Tink 암호화 알고리즘 (기본값: [TinkEncryptAlgorithm.AES256_GCM])
 */
@JacksonAnnotationsInside
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY)
annotation class JsonTinkEncrypt(
    val algorithm: TinkEncryptAlgorithm = TinkEncryptAlgorithm.AES256_GCM,
)
