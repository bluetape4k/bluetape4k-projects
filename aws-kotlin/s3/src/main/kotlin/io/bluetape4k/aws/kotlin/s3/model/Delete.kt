package io.bluetape4k.aws.kotlin.s3.model

import aws.sdk.kotlin.services.s3.model.Delete

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
fun deleteOf(quiet: Boolean, vararg keys: String): Delete {
    return Delete {
        this.objects = keys.map { it.toObjectIdentifier() }
        this.quiet = quiet
    }
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
fun deleteOf(quiet: Boolean, keys: Collection<String>): Delete {
    return Delete {
        this.objects = keys.map { it.toObjectIdentifier() }
        this.quiet = quiet
    }
}