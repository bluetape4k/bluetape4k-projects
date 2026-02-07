package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.Delete
import aws.sdk.kotlin.services.s3.model.ObjectIdentifier
import io.bluetape4k.support.requireNotEmpty

/**
 * S3 Object 를 삭제하는 [Delete] 객체를 생성합니다.
 *
 * ```
 * val delete = deleteOf("bucket", "key-1", "key-2")
 * ```
 *
 * @param quiet [Boolean] 값으로, 삭제 결과를 요약할지 여부를 설정합니다.
 * @param keys 삭제할 Object 의 키 목록
 * @return [Delete] 인스턴스
 */
@JvmName("deleteOfArray")
inline fun deleteOf(
    vararg keys: String,
    quiet: Boolean? = null,
    @BuilderInference crossinline builder: Delete.Builder.() -> Unit = {},
): Delete {
    return deleteOf(keys.map { it.toObjectIdentifier() }, quiet, builder)
}

/**
 * S3 Object 를 삭제하는 [Delete] 객체를 생성합니다.
 *
 * ```
 * val delete = deleteOf("bucket", "key-1", "key-2")
 * ```
 *
 * @param quiet [Boolean] 값으로, 삭제 결과를 요약할지 여부를 설정합니다.
 * @param keys 삭제할 Object 의 키 목록
 * @return [Delete] 인스턴스
 */
@JvmName("deleteOfCollection")
inline fun deleteOf(
    keys: Collection<String>,
    quiet: Boolean? = null,
    @BuilderInference crossinline builder: Delete.Builder.() -> Unit = {},
): Delete {
    return deleteOf(keys.map { it.toObjectIdentifier() }, quiet, builder)
}

/**
 * S3 Object 를 삭제하는 [Delete] 객체를 생성합니다.
 *
 * ```
 * val delete = deleteOf("bucket", "key-1", "key-2")
 * ```
 *
 * @param quiet [Boolean] 값으로, 삭제 결과를 요약할지 여부를 설정합니다.
 * @param keys 삭제할 Object 의 키 목록
 * @return [Delete] 인스턴스
 */
@JvmName("deleteOfObjectIdentifierArray")
inline fun deleteOf(
    vararg keys: ObjectIdentifier,
    quiet: Boolean? = null,
    @BuilderInference crossinline builder: Delete.Builder.() -> Unit = {},
): Delete {
    return deleteOf(keys.toList(), quiet, builder)
}


/**
 * S3 Object 를 삭제하는 [Delete] 객체를 생성합니다.
 *
 * ```
 * val deleteKeys = listOf("bucket", "key-1", "key-2").map { it.toObjectIdentifier() }
 * val delete = deleteOf(deleteKeys, quiet = true)
 * ```
 *
 * @param quiet [Boolean] 값으로, 삭제 결과를 요약할지 여부를 설정합니다.
 * @param keys 삭제할 Object 의 키 목록
 * @return [Delete] 인스턴스
 */
@JvmName("deleteOfObjectIdentifierCollection")
inline fun deleteOf(
    keys: Collection<ObjectIdentifier>,
    quiet: Boolean? = null,
    @BuilderInference crossinline builder: Delete.Builder.() -> Unit = {},
): Delete {
    keys.requireNotEmpty("keys")

    return Delete {
        this.objects = keys.toList()
        this.quiet = quiet

        builder()
    }
}
