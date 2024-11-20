package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.DeleteRequest
import aws.sdk.kotlin.services.dynamodb.model.PutRequest
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest

fun writeRequestOf(
    putRequest: PutRequest? = null,
    deleteRequest: DeleteRequest? = null,
): WriteRequest {
    require(putRequest != null || deleteRequest != null) {
        "Either putRequest or deleteRequest must be provided"
    }

    return WriteRequest {
        this.putRequest = putRequest
        this.deleteRequest = deleteRequest
    }
}

@JvmName("putRequestOfMap")
fun writePutRequestOf(item: Map<String, Any?>): WriteRequest = writeRequestOf(putRequestOf(item))

@JvmName("putRequestOfAttributeValue")
fun writePutRequestOf(item: Map<String, AttributeValue>): WriteRequest = writeRequestOf(putRequestOf(item))

@JvmName("deleteRequestOfMap")
fun writeDeleteRequestOf(key: Map<String, Any?>): WriteRequest =
    writeRequestOf(deleteRequest = deleteRequestOf(key))

@JvmName("deleteRequestOfAttributeValue")
fun writeDeleteRequestOf(key: Map<String, AttributeValue>): WriteRequest =
    writeRequestOf(deleteRequest = deleteRequestOf(key))
