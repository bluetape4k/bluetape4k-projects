package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.DeleteAliasRequest

/**
 * DSL 스타일의 빌더 람다로 [DeleteAliasRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [DeleteAliasRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = deleteAlias {
 *     aliasName("alias/sample")
 * }
 * // request.aliasName() == "alias/sample"
 * ```
 */
inline fun deleteAlias(
    builder: DeleteAliasRequest.Builder.() -> Unit,
): DeleteAliasRequest =
    DeleteAliasRequest.builder().apply(builder).build()

/**
 * Alias 이름을 지정하여 [DeleteAliasRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [aliasName]이 blank이면 `IllegalArgumentException`을 던집니다.
 * - 검증이 통과하면 [DeleteAliasRequest.Builder.aliasName]에 값을 설정합니다.
 *
 * ```kotlin
 * val request = deleteAliasOf("alias/sample")
 * // request.aliasName() == "alias/sample"
 * ```
 */
fun deleteAliasOf(aliasName: String): DeleteAliasRequest {
    aliasName.requireNotBlank("aliasName")
    return deleteAlias {
        aliasName(aliasName)
    }
}
