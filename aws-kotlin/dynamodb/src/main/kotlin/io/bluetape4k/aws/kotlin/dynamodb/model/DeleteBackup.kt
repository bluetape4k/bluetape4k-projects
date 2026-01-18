package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.DeleteBackupRequest
import io.bluetape4k.support.requireNotBlank

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
