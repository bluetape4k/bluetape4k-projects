package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.CreateBackupRequest
import io.bluetape4k.support.requireNotBlank

fun createBackupRequestOf(
    tableName: String,
    backupName: String,
    configurer: CreateBackupRequest.Builder.() -> Unit = {},
): CreateBackupRequest {
    tableName.requireNotBlank("tableName")
    backupName.requireNotBlank("backupName")

    return CreateBackupRequest {
        this.tableName = tableName
        this.backupName = backupName
        configurer()
    }
}