package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.services.kms.model.ListAliasesRequest

/**
 * DSL 스타일의 빌더 람다로 [ListAliasesRequest]를 생성합니다.
 *
 * @param builder [ListAliasesRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [ListAliasesRequest] 인스턴스.
 */
fun listAliasesRequest(
    @BuilderInference builder: ListAliasesRequest.Builder.() -> Unit,
): ListAliasesRequest =
    ListAliasesRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [ListAliasesRequest]를 생성합니다.
 *
 * @param keyId 특정 키의 Alias만 조회할 경우 키 ID 또는 ARN. null이면 전체 Alias를 조회합니다.
 * @param limit 반환할 최대 Alias 수.
 * @param marker 페이지네이션 마커 (이전 응답의 `nextMarker` 값).
 * @param builder [ListAliasesRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [ListAliasesRequest] 인스턴스.
 */
fun listAliasesRequestOf(
    keyId: String? = null,
    limit: Int? = null,
    marker: String? = null,
    @BuilderInference builder: ListAliasesRequest.Builder.() -> Unit = {},
): ListAliasesRequest {

    return listAliasesRequest {
        keyId?.let { keyId(it) }
        limit?.let { limit(it) }
        marker?.let { marker(it) }

        builder()
    }
}
