package io.bluetape4k.aws.kotlin.dynamodb.model

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.DeleteRequest
import aws.sdk.kotlin.services.dynamodb.model.PutRequest
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest

/**
 * [PutRequest]로 DynamoDB [WriteRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [putRequest]를 [WriteRequest]의 `putRequest` 필드에 설정한다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = writeRequestOf(putRequestOf(mapOf("id" to AttributeValue.S("u1"))))
 * // req.putRequest?.item?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param putRequest 저장 요청을 정의하는 [PutRequest] 객체
 */
@JvmName("writeRequestOfPut")
inline fun writeRequestOf(
    putRequest: PutRequest,
    @BuilderInference crossinline builder: WriteRequest.Builder.() -> Unit = {},
): WriteRequest = WriteRequest {
    this.putRequest = putRequest
    builder()
}

/**
 * [DeleteRequest]로 DynamoDB [WriteRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [deleteRequest]를 [WriteRequest]의 `deleteRequest` 필드에 설정한다.
 * - [builder] 블록으로 추가 필드를 덮어쓸 수 있다.
 *
 * ```kotlin
 * val req = writeRequestOf(deleteRequestOf(mapOf("id" to AttributeValue.S("u1"))))
 * // req.deleteRequest?.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param deleteRequest 삭제 요청을 정의하는 [DeleteRequest] 객체
 */
@JvmName("writeRequestOfDelete")
inline fun writeRequestOf(
    deleteRequest: DeleteRequest,
    @BuilderInference crossinline builder: WriteRequest.Builder.() -> Unit = {},
): WriteRequest = WriteRequest {
    this.deleteRequest = deleteRequest
    builder()
}

/**
 * Any? 속성 맵으로 Put 타입의 DynamoDB [WriteRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [item]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 *
 * ```kotlin
 * val req = writePutRequestOf(mapOf("id" to "u1"))
 * // req.putRequest?.item?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param item 저장할 항목의 속성 맵 (자동으로 [AttributeValue]로 변환)
 */
@JvmName("putRequestOfMap")
fun writePutRequestOf(item: Map<String, Any?>): WriteRequest =
    writeRequestOf(putRequestOf(item))

/**
 * [AttributeValue] 속성 맵으로 Put 타입의 DynamoDB [WriteRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [item]은 변환 없이 직접 [PutRequest]의 항목으로 설정된다.
 *
 * ```kotlin
 * val req = writePutRequestOf(mapOf("id" to AttributeValue.S("u1")))
 * // req.putRequest?.item?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param item 저장할 항목의 [AttributeValue] 속성 맵
 */
@JvmName("putRequestOfAttributeValue")
fun writePutRequestOf(item: Map<String, AttributeValue>): WriteRequest =
    writeRequestOf(putRequestOf(item))

/**
 * Any? 키 맵으로 Delete 타입의 DynamoDB [WriteRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [key]의 각 값은 [toAttributeValueMap]을 통해 [AttributeValue]로 자동 변환된다.
 *
 * ```kotlin
 * val req = writeDeleteRequestOf(mapOf("id" to "u1"))
 * // req.deleteRequest?.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param key 삭제할 항목의 기본 키 맵 (자동으로 [AttributeValue]로 변환)
 */
@JvmName("deleteRequestOfMap")
fun writeDeleteRequestOf(key: Map<String, Any?>): WriteRequest =
    writeRequestOf(deleteRequest = deleteRequestOf(key))

/**
 * [AttributeValue] 키 맵으로 Delete 타입의 DynamoDB [WriteRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [key]는 변환 없이 직접 [DeleteRequest]의 키로 설정된다.
 *
 * ```kotlin
 * val req = writeDeleteRequestOf(mapOf("id" to AttributeValue.S("u1")))
 * // req.deleteRequest?.key?.get("id") == AttributeValue.S("u1")
 * ```
 *
 * @param key 삭제할 항목의 [AttributeValue] 기본 키 맵
 */
@JvmName("deleteRequestOfAttributeValue")
fun writeDeleteRequestOf(key: Map<String, AttributeValue>): WriteRequest =
    writeRequestOf(deleteRequest = deleteRequestOf(key))
