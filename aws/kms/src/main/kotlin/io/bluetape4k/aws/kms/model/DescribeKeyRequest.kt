package io.bluetape4k.aws.kms.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest

/**
 * DSL 스타일의 빌더 람다로 [DescribeKeyRequest]를 생성합니다.
 *
 * @param builder [DescribeKeyRequest.Builder]에 대한 설정 람다.
 * @return 설정된 [DescribeKeyRequest] 인스턴스.
 */
inline fun describeKey(
    @BuilderInference builder: DescribeKeyRequest.Builder.() -> Unit,
): DescribeKeyRequest =
    DescribeKeyRequest.builder().apply(builder).build()

/**
 * 키 ID를 지정하여 [DescribeKeyRequest]를 생성합니다.
 *
 * @param keyId 조회할 KMS 키의 ID, ARN, 또는 Alias 이름. 공백 불가.
 * @param grantTokens 키 메타데이터 조회에 사용할 Grant 토큰 목록.
 * @param builder [DescribeKeyRequest.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [DescribeKeyRequest] 인스턴스.
 */
fun describeKeyOf(
    keyId: String,
    vararg grantTokens: String = emptyArray(),
    @BuilderInference builder: DescribeKeyRequest.Builder.() -> Unit = {},
): DescribeKeyRequest {
    keyId.requireNotBlank("keyId")

    return describeKey {
        keyId(keyId)
        if (grantTokens.isNotEmpty()) {
            grantTokens(*grantTokens)
        }

        builder()
    }
}
