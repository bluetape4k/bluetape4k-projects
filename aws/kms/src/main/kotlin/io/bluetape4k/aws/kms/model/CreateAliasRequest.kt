package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.CreateAliasRequest

/**
 * DSL 스타일의 빌더 람다로 [CreateAliasRequest]를 생성합니다.
 *
 * @param builder [CreateAliasRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [CreateAliasRequest] 인스턴스.
 */
inline fun createAliasRequest(
    @BuilderInference builder: CreateAliasRequest.Builder.() -> Unit,
): CreateAliasRequest =
    CreateAliasRequest.builder().apply(builder).build()

/**
 * Alias 이름과 대상 키 ID를 지정하여 [CreateAliasRequest]를 생성합니다.
 *
 * @param aliasName 생성할 Alias 이름. 반드시 "alias/" 접두사로 시작해야 합니다. 공백 불가.
 * @param targetKeyId Alias가 가리킬 KMS 키의 ID 또는 ARN. 공백 불가.
 * @param builder [CreateAliasRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [CreateAliasRequest] 인스턴스.
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
