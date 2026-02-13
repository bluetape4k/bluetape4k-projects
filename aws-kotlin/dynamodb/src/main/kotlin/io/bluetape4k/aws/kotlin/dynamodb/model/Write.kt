package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.DeleteRequest
import aws.sdk.kotlin.services.dynamodb.model.PutRequest
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest

@JvmName("writeRequestOfPut")
inline fun writeRequestOf(
    putRequest: PutRequest,
    @BuilderInference crossinline builder: WriteRequest.Builder.() -> Unit = {},
): WriteRequest = WriteRequest {
    this.putRequest = putRequest
    builder()
}

@JvmName("writeRequestOfDelete")
inline fun writeRequestOf(
    deleteRequest: DeleteRequest,
    @BuilderInference crossinline builder: WriteRequest.Builder.() -> Unit = {},
): WriteRequest = WriteRequest {
    this.deleteRequest = deleteRequest
    builder()
}

@JvmName("putRequestOfMap")
fun writePutRequestOf(item: Map<String, Any?>): WriteRequest =
    writeRequestOf(putRequestOf(item))

@JvmName("putRequestOfAttributeValue")
fun writePutRequestOf(item: Map<String, AttributeValue>): WriteRequest =
    writeRequestOf(putRequestOf(item))

@JvmName("deleteRequestOfMap")
fun writeDeleteRequestOf(key: Map<String, Any?>): WriteRequest =
    writeRequestOf(deleteRequest = deleteRequestOf(key))

@JvmName("deleteRequestOfAttributeValue")
fun writeDeleteRequestOf(key: Map<String, AttributeValue>): WriteRequest =
    writeRequestOf(deleteRequest = deleteRequestOf(key))
