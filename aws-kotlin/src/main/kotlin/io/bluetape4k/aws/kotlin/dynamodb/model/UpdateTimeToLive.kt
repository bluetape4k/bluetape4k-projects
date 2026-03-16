package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.TimeToLiveSpecification
import aws.sdk.kotlin.services.dynamodb.model.UpdateTimeToLiveRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [UpdateTimeToLiveRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [timeToLiveSpecification]이 null이면 TTL 설정 없이 요청이 생성된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = updateTimeToLiveRequestOf(
 *     tableName = "users",
 *     timeToLiveSpecification = TimeToLiveSpecification {
 *         enabled = true
 *         attributeName = "expiresAt"
 *     }
 * )
 * // req.tableName == "users"
 * // req.timeToLiveSpecification?.enabled == true
 * ```
 *
 * @param tableName TTL을 업데이트할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param timeToLiveSpecification TTL 활성화 및 속성 이름 설정
 */
inline fun updateTimeToLiveRequestOf(
    tableName: String,
    timeToLiveSpecification: TimeToLiveSpecification? = null,
    @BuilderInference crossinline builder: UpdateTimeToLiveRequest.Builder.() -> Unit = {},
): UpdateTimeToLiveRequest {
    tableName.requireNotBlank("tableName")

    return UpdateTimeToLiveRequest {
        this.tableName = tableName
        this.timeToLiveSpecification = timeToLiveSpecification

        builder()
    }
}
