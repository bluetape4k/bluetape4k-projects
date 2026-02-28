package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.DeleteAliasRequest

/**
 * DSL 스타일의 빌더 람다로 [DeleteAliasRequest]를 생성합니다.
 *
 * @param builder [DeleteAliasRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [DeleteAliasRequest] 인스턴스.
 */
inline fun deleteAlias(
    @BuilderInference builder: DeleteAliasRequest.Builder.() -> Unit,
): DeleteAliasRequest =
    DeleteAliasRequest.builder().apply(builder).build()

/**
 * Alias 이름을 지정하여 [DeleteAliasRequest]를 생성합니다.
 *
 * @param aliasName 삭제할 Alias 이름. "alias/" 접두사로 시작해야 합니다. 공백 불가.
 * @return 설정된 [DeleteAliasRequest] 인스턴스.
 */
fun deleteAliasOf(aliasName: String): DeleteAliasRequest {
    aliasName.requireNotBlank("aliasName")
    return deleteAlias {
        aliasName(aliasName)
    }
}
