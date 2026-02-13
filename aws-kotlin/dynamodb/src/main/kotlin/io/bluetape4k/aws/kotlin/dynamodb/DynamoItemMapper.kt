package io.bluetape4k.aws.kotlin.dynamodb

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.WriteRequest

/**
 * 엔티티를 DynamoDB Item으로 변환하는 Mapper 입니다.
 */
fun interface DynamoItemMapper<T: Any> {

    fun mapToDynamoItem(item: T): Map<String, AttributeValue>
}

/**
 * Iterable의 엔티티들을 DynamoDB 쓰기 (Put) 작업 요청인 [WriteRequest]의 컬렉션으로 변환합니다.
 *
 * ```
 * val writeRequests = items.buildWriteRequests(mapper)
 * ```
 */
fun <T: Any> Iterable<T>.buildWritePutRequests(mapper: DynamoItemMapper<T>): List<WriteRequest> {
    return map {
        WriteRequest {
            putRequest {
                item = mapper.mapToDynamoItem(it)
            }
        }
    }
}

/**
 * Iterable의 엔티티들을 DynamoDB 삭제 (Delete) 작업 요청인 [WriteRequest]의 컬렉션으로 변환합니다.
 *
 * ```
 * val writeRequests = items.buildWriteDeleteRequests(mapper)
 * ```
 */
fun <T: Any> Iterable<T>.buildWriteDeleteRequests(keySelector: DynamoItemMapper<T>): List<WriteRequest> {
    return this.map {
        WriteRequest {
            deleteRequest {
                key = keySelector.mapToDynamoItem(it)
            }
        }
    }
}
