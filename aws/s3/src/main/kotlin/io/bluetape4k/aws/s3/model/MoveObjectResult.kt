package io.bluetape4k.aws.s3.model

import software.amazon.awssdk.services.s3.model.CopyObjectResult
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse

/**
 * S3 객체 이동 작업의 결과를 나타냅니다.
 *
 * @property copyResult 복사 작업 결과
 * @property deleteResult 삭제 작업 결과 (null 이면 삭제가 수행되지 않음)
 * @property isSuccess 전체 작업 성공 여부
 */
data class MoveObjectResult(
    val copyResult: CopyObjectResult,
    val deleteResult: DeleteObjectResponse? = null,
) {
    /**
     * 복사와 삭제가 모두 성공했는지 여부
     */
    val isSuccess: Boolean
        get() = copyResult.eTag()?.isNotBlank() == true && deleteResult != null

    /**
     * 복사만 성공하고 삭제는 실패한 경우 (원자성 위반)
     */
    val isPartialSuccess: Boolean
        get() = copyResult.eTag()?.isNotBlank() == true && deleteResult == null
}
