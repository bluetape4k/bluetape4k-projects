package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.ListGrantsRequest

/**
 * DSL 스타일의 빌더 람다로 [ListGrantsRequest]를 생성합니다.
 *
 * @param builder [ListGrantsRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [ListGrantsRequest] 인스턴스.
 */
inline fun listGrantsRequest(
    @BuilderInference builder: ListGrantsRequest.Builder.() -> Unit,
): ListGrantsRequest =
    ListGrantsRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [ListGrantsRequest]를 생성합니다.
 *
 * @param keyId Grant 목록을 조회할 KMS 키의 ID 또는 ARN. 공백 불가.
 * @param grantId 특정 Grant만 조회할 경우 Grant ID.
 * @param marker 페이지네이션 마커 (이전 응답의 `nextMarker` 값).
 * @param limit 반환할 최대 Grant 수.
 * @param builder [ListGrantsRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [ListGrantsRequest] 인스턴스.
 */
fun listGrantsRequestOf(
    keyId: String,
    grantId: String? = null,
    marker: String? = null,
    limit: Int? = null,
    @BuilderInference builder: ListGrantsRequest.Builder.() -> Unit = {},
): ListGrantsRequest {
    keyId.requireNotBlank("keyId")

    return listGrantsRequest {
        keyId(keyId)
        grantId?.let { grantId(it) }
        marker?.let { marker(it) }
        limit?.let { limit(it) }

        builder()
    }
}
