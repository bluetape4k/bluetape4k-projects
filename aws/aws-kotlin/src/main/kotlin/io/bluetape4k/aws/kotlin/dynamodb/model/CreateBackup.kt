package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.CreateBackupRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [CreateBackupRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [tableName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [backupName]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = createBackupRequestOf("users", "users-backup-2024")
 * // req.tableName == "users"
 * // req.backupName == "users-backup-2024"
 * ```
 *
 * @param tableName 백업할 DynamoDB 테이블 이름 (blank이면 예외)
 * @param backupName 생성할 백업 이름 (blank이면 예외)
 */
inline fun createBackupRequestOf(
    tableName: String,
    backupName: String,
    crossinline builder: CreateBackupRequest.Builder.() -> Unit = {},
): CreateBackupRequest {
    tableName.requireNotBlank("tableName")
    backupName.requireNotBlank("backupName")

    return CreateBackupRequest {
        this.tableName = tableName
        this.backupName = backupName

        builder()
    }
}
