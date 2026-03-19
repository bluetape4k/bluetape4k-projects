package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AutoScalingSettingsUpdate
import aws.sdk.kotlin.services.dynamodb.model.GlobalTableGlobalSecondaryIndexSettingsUpdate
import aws.sdk.kotlin.services.dynamodb.model.ReplicaUpdate
import aws.sdk.kotlin.services.dynamodb.model.UpdateGlobalTableRequest
import aws.sdk.kotlin.services.dynamodb.model.UpdateGlobalTableSettingsRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [UpdateGlobalTableRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [globalTableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [replicaUpdates]가 null이면 복제본 업데이트 없이 요청이 생성된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = updateGlobalTableRequestOf(
 *     globalTableName = "global-users",
 *     replicaUpdates = listOf(replicaUpdateOf(create = CreateReplicaAction { regionName = "us-west-2" }))
 * )
 * // req.globalTableName == "global-users"
 * // req.replicaUpdates?.size == 1
 * ```
 *
 * @param globalTableName 업데이트할 글로벌 테이블 이름 (blank이면 예외)
 * @param replicaUpdates 복제본 추가/삭제 업데이트 목록
 */
inline fun updateGlobalTableRequestOf(
    globalTableName: String,
    replicaUpdates: List<ReplicaUpdate>?,
    crossinline builder: UpdateGlobalTableRequest.Builder.() -> Unit = {},
): UpdateGlobalTableRequest {
    globalTableName.requireNotBlank("globalTableName")

    return UpdateGlobalTableRequest {
        this.globalTableName = globalTableName
        this.replicaUpdates = replicaUpdates

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [UpdateGlobalTableSettingsRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [globalTableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - null로 전달된 설정 값은 요청에 포함되지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = updateGlobalTableSettingsRequestOf(
 *     globalTableName = "global-users",
 *     autoScalingSettingsUpdate = AutoScalingSettingsUpdate { minimumUnits = 5L }
 * )
 * // req.globalTableName == "global-users"
 * ```
 *
 * @param globalTableName 설정을 업데이트할 글로벌 테이블 이름 (blank이면 예외)
 * @param autoScalingSettingsUpdate 쓰기 용량 오토스케일링 설정
 * @param gsIndexSettingsUpdates 글로벌 보조 인덱스 설정 업데이트 목록
 */
inline fun updateGlobalTableSettingsRequestOf(
    globalTableName: String,
    autoScalingSettingsUpdate: AutoScalingSettingsUpdate? = null,
    gsIndexSettingsUpdates: List<GlobalTableGlobalSecondaryIndexSettingsUpdate>? = null,
    crossinline builder: UpdateGlobalTableSettingsRequest.Builder.() -> Unit = {},
): UpdateGlobalTableSettingsRequest {
    globalTableName.requireNotBlank("globalTableName")

    return UpdateGlobalTableSettingsRequest {
        this.globalTableName = globalTableName
        this.globalTableProvisionedWriteCapacityAutoScalingSettingsUpdate = autoScalingSettingsUpdate
        this.globalTableGlobalSecondaryIndexSettingsUpdate = gsIndexSettingsUpdates

        builder()
    }
}
