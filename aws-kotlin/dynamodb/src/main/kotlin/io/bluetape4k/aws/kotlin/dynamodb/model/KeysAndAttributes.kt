package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.KeysAndAttributes
import io.bluetape4k.support.requireNotEmpty

/**
 * DSL 블록으로 DynamoDB [KeysAndAttributes]를 빌드합니다 ([AttributeValue] 키 오버로드).
 *
 * ## 동작/계약
 * - [keys]가 비어 있으면 `IllegalArgumentException`을 던진다.
 * - [builder] 블록으로 프로젝션 표현식 등 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val kaa = keysAndAttributesOf(
 *     listOf(mapOf("id" to AttributeValue.S("u1")), mapOf("id" to AttributeValue.S("u2")))
 * )
 * // kaa.keys?.size == 2
 * ```
 *
 * @param keys 조회할 항목의 기본 키 맵 목록 (비어 있으면 예외)
 */
@JvmName("keysAndAttributesOfAttributeValue")
inline fun keysAndAttributesOf(
    keys: List<Map<String, AttributeValue>>,
    @BuilderInference crossinline builder: KeysAndAttributes.Builder.() -> Unit = {},
): KeysAndAttributes {
    keys.requireNotEmpty("keys")

    return KeysAndAttributes {
        this.keys = keys

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [KeysAndAttributes]를 빌드합니다 (Any? 키 오버로드).
 *
 * ## 동작/계약
 * - [keys]가 비어 있으면 `IllegalArgumentException`을 던진다.
 * - 각 키 맵의 값은 [toAttributeValue]를 통해 [AttributeValue]로 자동 변환된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val kaa = keysAndAttributesOf(listOf(mapOf("id" to "u1"), mapOf("id" to "u2")))
 * // kaa.keys?.first()?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param keys 조회할 항목의 기본 키 맵 목록 (비어 있으면 예외, 자동으로 [AttributeValue]로 변환)
 */
@JvmName("keysAndAttributesOfAny")
inline fun keysAndAttributesOf(
    keys: List<Map<String, Any?>>,
    @BuilderInference crossinline builder: KeysAndAttributes.Builder.() -> Unit = {},
): KeysAndAttributes {
    keys.requireNotEmpty("keys")

    return keysAndAttributesOf(
        keys.map { it.mapValues { it.value.toAttributeValue() } },
        builder
    )
}
