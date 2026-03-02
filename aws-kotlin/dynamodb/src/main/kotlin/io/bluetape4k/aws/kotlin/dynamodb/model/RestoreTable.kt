package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.RestoreTableFromBackupRequest
import aws.sdk.kotlin.services.dynamodb.model.RestoreTableToPointInTimeRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [RestoreTableFromBackupRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [backupArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [targetTableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = restoreTableFromBackupRequestOf(
 *     backupArn = "arn:aws:dynamodb:us-east-1:123456789012:table/users/backup/01234567890123-abc",
 *     targetTableName = "users-restored"
 * )
 * // req.targetTableName == "users-restored"
 * ```
 *
 * @param backupArn 복원 원본 백업의 ARN (blank이면 예외)
 * @param targetTableName 복원 대상 테이블 이름 (blank이면 예외)
 */
inline fun restoreTableFromBackupRequestOf(
    backupArn: String,
    targetTableName: String,
    @BuilderInference crossinline builder: RestoreTableFromBackupRequest.Builder.() -> Unit = {},
): RestoreTableFromBackupRequest {
    backupArn.requireNotBlank("backupArn")
    targetTableName.requireNotBlank("targetTableName")

    return RestoreTableFromBackupRequest {
        this.backupArn = backupArn
        this.targetTableName = targetTableName

        builder()
    }
}

/**
 * DSL 블록으로 DynamoDB [RestoreTableToPointInTimeRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [targetTableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [sourceTableArn] 또는 [sourceTableName] 중 하나를 지정하여 복원 원본을 설정한다.
 * - [useLatestRestorableTime]이 true이면 가장 최근 복원 가능 시점으로 복원한다.
 *
 * ```kotlin
 * val req = restoreTableToPointInTimeRequestOf(
 *     sourceTableName = "users",
 *     targetTableName = "users-restored",
 *     useLatestRestorableTime = true
 * ) {}
 * // req.targetTableName == "users-restored"
 * // req.useLatestRestorableTime == true
 * ```
 *
 * @param sourceTableArn 복원 원본 테이블의 ARN
 * @param sourceTableName 복원 원본 테이블 이름
 * @param targetTableName 복원 대상 테이블 이름 (blank이면 예외)
 * @param useLatestRestorableTime 가장 최근 복원 가능 시점 사용 여부
 */
inline fun restoreTableToPointInTimeRequestOf(
    sourceTableArn: String? = null,
    sourceTableName: String? = null,
    targetTableName: String? = null,
    useLatestRestorableTime: Boolean? = null,
    @BuilderInference crossinline builder: RestoreTableToPointInTimeRequest.Builder.() -> Unit,
): RestoreTableToPointInTimeRequest {

    targetTableName.requireNotBlank("targetTableName")

    return RestoreTableToPointInTimeRequest {
        this.sourceTableArn = sourceTableArn
        this.sourceTableName = sourceTableName
        this.targetTableName = targetTableName
        this.useLatestRestorableTime = useLatestRestorableTime

        builder()
    }
}
