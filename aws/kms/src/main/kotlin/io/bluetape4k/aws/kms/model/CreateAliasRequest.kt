package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.CreateAliasRequest

/**
 * DSL 스타일의 빌더 람다로 [CreateAliasRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [CreateAliasRequest.builder]에 [builder]를 적용한 뒤 `build()`를 호출합니다.
 *
 * ```kotlin
 * val request = createAliasRequest {
 *     aliasName("alias/sample")
 *     targetKeyId("key-id")
 * }
 * // request.aliasName() == "alias/sample"
 * ```
 */
inline fun createAliasRequest(
    @BuilderInference builder: CreateAliasRequest.Builder.() -> Unit,
): CreateAliasRequest =
    CreateAliasRequest.builder().apply(builder).build()

/**
 * Alias 이름과 대상 키 ID를 지정하여 [CreateAliasRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [aliasName], [targetKeyId]가 blank이면 `IllegalArgumentException`을 던집니다.
 * - 검증이 통과하면 두 필드를 설정하고 [builder]를 추가로 실행합니다.
 *
 * ```kotlin
 * val request = createAliasRequestOf(
 *     aliasName = "alias/sample",
 *     targetKeyId = "key-id"
 * )
 * // request.targetKeyId() == "key-id"
 * ```
 */
fun createAliasRequestOf(
    aliasName: String,
    targetKeyId: String,
    @BuilderInference builder: CreateAliasRequest.Builder.() -> Unit = {},
): CreateAliasRequest {
    aliasName.requireNotBlank("aliasName")
    targetKeyId.requireNotBlank("targetKeyId")

    return createAliasRequest {
        aliasName(aliasName)
        targetKeyId(targetKeyId)

        builder()
    }
}
