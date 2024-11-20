package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.Put
import aws.sdk.kotlin.services.dynamodb.model.TransactWriteItem

fun transactWriteItemOf(put: Put): TransactWriteItem {
    return TransactWriteItem {
        this.put = put

    }
}
