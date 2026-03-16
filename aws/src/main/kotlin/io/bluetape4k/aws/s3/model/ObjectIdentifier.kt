package io.bluetape4k.aws.s3.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.s3.model.ObjectIdentifier

/**
 * [ObjectIdentifier]를 생성합니다.
 *
 * [key]는 공백일 수 없습니다.
 *
 * 예제:
 * ```kotlin
 * val result = objectIdentifier("docs/readme.md")
 * // result.key() == "docs/readme.md"
 * ```
 */
inline fun objectIdentifier(
    key: String,
    @BuilderInference builder: ObjectIdentifier.Builder.() -> Unit = {},
): ObjectIdentifier {
    key.requireNotBlank("key")
    return ObjectIdentifier.builder()
        .key(key)
        .apply(builder)
        .build()
}

/**
 * [ObjectIdentifier]를 생성하고 선택적으로 [versionId]를 지정합니다.
 *
 * 예제:
 * ```kotlin
 * val result = objectIdentifierOf("docs/readme.md", versionId = "v3")
 * // result.versionId() == "v3"
 * ```
 */
inline fun objectIdentifierOf(
    key: String,
    versionId: String? = null,
    @BuilderInference builder: ObjectIdentifier.Builder.() -> Unit = {},
): ObjectIdentifier =
    objectIdentifier(key) {
        versionId(versionId)
        builder()
    }
