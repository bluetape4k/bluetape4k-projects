package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.ObjectIdentifier
import io.bluetape4k.support.requireNotBlank

/**
 * [key]를 object identifier로 가지는 [ObjectIdentifier] 를 생성합니다.
 *
 * ```
 * val identifier = objectIdentifierOf("key")
 * ```
 *
 * @param key [String] 값으로, object identifier의 key 값을 설정합니다.
 * @param versionId [String] 값으로, object identifier의 version id 값을 설정합니다.
 *
 * @return [ObjectIdentifier] 인스턴스
 */
inline fun objectIdentifierOf(
    key: String,
    versionId: String? = null,
    crossinline builder: ObjectIdentifier.Builder.() -> Unit = {},
): ObjectIdentifier {
    key.requireNotBlank("key")

    return ObjectIdentifier {
        this.key = key
        this.versionId = versionId

        builder()
    }
}

/**
 * 문자열을 object identifier로 가지는 [ObjectIdentifier] 를 생성합니다.
 *
 * ```
 * val identifier = "key".toObjectIdentifier()
 * ```
 *
 * @receiver [String] 값으로, object identifier의 key 값을 설정합니다.
 * @param versionId [String] 값으로, object identifier의 version id 값을 설정합니다.
 *
 * @return [ObjectIdentifier] 인스턴스
 */
fun String.toObjectIdentifier(
    versionId: String? = null,
    block: ObjectIdentifier.Builder.() -> Unit = {},
): ObjectIdentifier {
    return objectIdentifierOf(this, versionId, block)
}
