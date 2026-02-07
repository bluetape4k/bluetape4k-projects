package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.ConditionCheck
import aws.sdk.kotlin.services.dynamodb.model.Delete
import aws.sdk.kotlin.services.dynamodb.model.Put
import aws.sdk.kotlin.services.dynamodb.model.TransactWriteItem
import aws.sdk.kotlin.services.dynamodb.model.Update

fun transactWriteItemOf(put: Put): TransactWriteItem =
    TransactWriteItem {
        this.put = put
    }

inline fun transactWriteItemOf(
    name: String,
    item: Map<String, AttributeValue> = emptyMap(),
    crossinline putBuilder: Put.Builder.() -> Unit,
): TransactWriteItem =
    transactWriteItemOf(putOf(name, item, putBuilder))


fun transactWriteItemOf(
    conditionCheck: ConditionCheck? = null,
    delete: Delete? = null,
    put: Put? = null,
    update: Update? = null,
    @BuilderInference builder: TransactWriteItem.Builder.() -> Unit = {},
): TransactWriteItem =
    TransactWriteItem {
        this.conditionCheck = conditionCheck
        this.put = put
        this.update = update
        this.delete = delete

        builder()
    }
