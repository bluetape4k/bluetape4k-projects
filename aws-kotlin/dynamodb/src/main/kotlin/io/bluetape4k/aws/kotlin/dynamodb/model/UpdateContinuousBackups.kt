package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.PointInTimeRecoverySpecification
import aws.sdk.kotlin.services.dynamodb.model.UpdateContinuousBackupsRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [UpdateContinuousBackupsRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [pointInTimeRecoverySpecification]이 null이면 PITR 설정 없이 요청이 생성된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = updateContinuousBackupsRequestOf(
 *     tableName = "users",
 *     pointInTimeRecoverySpecification = PointInTimeRecoverySpecification { pointInTimeRecoveryEnabled = true }
 * )
 * // req.tableName == "users"
 * // req.pointInTimeRecoverySpecification?.pointInTimeRecoveryEnabled == true
 * ```
 *
 * @param tableName PITR 설정을 업데이트할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param pointInTimeRecoverySpecification 특정 시점 복구(PITR) 활성화 여부 설정
 */
inline fun updateContinuousBackupsRequestOf(
    tableName: String,
    pointInTimeRecoverySpecification: PointInTimeRecoverySpecification? = null,
    @BuilderInference crossinline builder: UpdateContinuousBackupsRequest.Builder.() -> Unit = {},
): UpdateContinuousBackupsRequest {
    tableName.requireNotBlank("tableName")

    return UpdateContinuousBackupsRequest {
        this.tableName = tableName
        this.pointInTimeRecoverySpecification = pointInTimeRecoverySpecification

        builder()
    }
}
