package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.CreateBackupRequest
import io.bluetape4k.support.requireNotBlank

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
