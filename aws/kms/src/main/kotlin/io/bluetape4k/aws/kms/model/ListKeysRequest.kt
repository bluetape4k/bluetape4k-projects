package io.bluetape4k.aws.kms.model

import software.amazon.awssdk.services.kms.model.ListKeysRequest

/**
 * DSL 스타일의 빌더 람다로 [ListKeysRequest]를 생성합니다.
 *
 * @param builder [ListKeysRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [ListKeysRequest] 인스턴스.
 */
inline fun listKeysRequest(
    @BuilderInference builder: ListKeysRequest.Builder.() -> Unit,
): ListKeysRequest =
    ListKeysRequest.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [ListKeysRequest]를 생성합니다.
 *
 * @param limit 반환할 최대 키 수.
 * @param marker 페이지네이션 마커 (이전 응답의 `nextMarker` 값).
 * @param builder [ListKeysRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [ListKeysRequest] 인스턴스.
 */
fun listKeysRequestOf(
    limit: Int? = null,
    marker: String? = null,
    @BuilderInference builder: ListKeysRequest.Builder.() -> Unit = {},
): ListKeysRequest {

    return listKeysRequest {
        limit?.let { limit(it) }
        marker?.let { marker(it) }

        builder()
    }
}
