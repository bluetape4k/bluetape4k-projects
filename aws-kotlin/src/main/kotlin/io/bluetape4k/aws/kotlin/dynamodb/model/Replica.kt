package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AutoScalingSettingsUpdate
import aws.sdk.kotlin.services.dynamodb.model.CreateReplicaAction
import aws.sdk.kotlin.services.dynamodb.model.DeleteReplicaAction
import aws.sdk.kotlin.services.dynamodb.model.Replica
import aws.sdk.kotlin.services.dynamodb.model.ReplicaGlobalSecondaryIndexSettingsUpdate
import aws.sdk.kotlin.services.dynamodb.model.ReplicaSettingsUpdate
import aws.sdk.kotlin.services.dynamodb.model.ReplicaUpdate
import aws.sdk.kotlin.services.dynamodb.model.TableClass
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [Replica]를 빌드합니다.
 *
 * ## 동작/계약
 * - [regionName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val replica = replicaOf("ap-northeast-2")
 * // replica.regionName == "ap-northeast-2"
 * ```
 *
 * @param regionName 복제할 AWS 리전 이름 (blank이면 예외)
 */
inline fun replicaOf(
    regionName: String,
    crossinline builder: Replica.Builder.() -> Unit = {},
): Replica {
    regionName.requireNotBlank("regionName")

    return Replica {
        this.regionName = regionName

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [ReplicaUpdate]를 빌드합니다.
 *
 * ## 동작/계약
 * - [create]와 [delete] 중 하나를 지정하여 복제본 추가 또는 삭제 작업을 설정한다.
 * - 둘 다 null이면 빈 [ReplicaUpdate]가 생성된다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val update = replicaUpdateOf(create = CreateReplicaAction { regionName = "us-west-2" })
 * // update.create?.regionName == "us-west-2"
 * ```
 *
 * @param create 추가할 복제본 리전 설정
 * @param delete 삭제할 복제본 리전 설정
 */
inline fun replicaUpdateOf(
    create: CreateReplicaAction? = null,
    delete: DeleteReplicaAction? = null,
    crossinline builder: ReplicaUpdate.Builder.() -> Unit = {},
): ReplicaUpdate = ReplicaUpdate {
    this.create = create
    this.delete = delete

    builder()
}

/**
 * DSL 블록으로 DynamoDB [ReplicaSettingsUpdate]를 빌드합니다.
 *
 * ## 동작/계약
 * - [regionName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - null로 전달된 설정 값은 요청에 포함되지 않는다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val settings = replicaSettingsUpdateOf(
 *     regionName = "ap-northeast-2",
 *     replicaProvisionedReadCapacityUnits = 10L
 * )
 * // settings.regionName == "ap-northeast-2"
 * // settings.replicaProvisionedReadCapacityUnits == 10L
 * ```
 *
 * @param regionName 설정을 업데이트할 복제본의 AWS 리전 이름 (blank이면 예외)
 * @param replicaGlobalSecondaryIndexSettingsUpdate GSI 설정 업데이트 목록
 * @param replicaProvisionedReadCapacityAutoScalingSettingsUpdate 읽기 용량 오토스케일링 설정
 * @param replicaProvisionedReadCapacityUnits 프로비저닝된 읽기 용량 단위
 * @param replicaTableClass 복제본 테이블 클래스
 */
inline fun replicaSettingsUpdateOf(
    regionName: String,
    replicaGlobalSecondaryIndexSettingsUpdate: List<ReplicaGlobalSecondaryIndexSettingsUpdate>? = null,
    replicaProvisionedReadCapacityAutoScalingSettingsUpdate: AutoScalingSettingsUpdate? = null,
    replicaProvisionedReadCapacityUnits: Long? = null,
    replicaTableClass: TableClass? = null,
    crossinline builder: ReplicaSettingsUpdate.Builder.() -> Unit = {},
): ReplicaSettingsUpdate {
    regionName.requireNotBlank("regionName")

    return ReplicaSettingsUpdate.invoke {
        this.regionName = regionName
        this.replicaGlobalSecondaryIndexSettingsUpdate = replicaGlobalSecondaryIndexSettingsUpdate
        this.replicaProvisionedReadCapacityAutoScalingSettingsUpdate =
            replicaProvisionedReadCapacityAutoScalingSettingsUpdate
        this.replicaProvisionedReadCapacityUnits = replicaProvisionedReadCapacityUnits
        this.replicaTableClass = replicaTableClass

        builder()
    }
}
