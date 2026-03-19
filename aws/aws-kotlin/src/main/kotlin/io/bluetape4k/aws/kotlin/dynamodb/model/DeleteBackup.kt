package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.DeleteBackupRequest
import io.bluetape4k.support.requireNotBlank

/**
 * DSL 블록으로 DynamoDB [DeleteBackupRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [backupArn]이 blank이면 `IllegalArgumentException`을 던진다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = deleteBackupRequestOf("arn:aws:dynamodb:us-east-1:123456789012:table/users/backup/01234567890123-abc")
 * // req.backupArn == "arn:aws:dynamodb:us-east-1:123456789012:table/users/backup/01234567890123-abc"
 * ```
 *
 * @param backupArn 삭제할 백업의 ARN (blank이면 예외)
 */
inline fun deleteBackupRequestOf(
    backupArn: String,
    crossinline builder: DeleteBackupRequest.Builder.() -> Unit = {},
): DeleteBackupRequest {
    backupArn.requireNotBlank("backupArn")

    return DeleteBackupRequest {
        this.backupArn = backupArn

        builder()
    }
}
