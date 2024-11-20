package io.bluetape4k.aws.dynamodb

import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.WriteRequest

/**
 * 엔티티를 DynamoDB Item으로 변환하는 Mapper 입니다.
 */
interface DynamoItemMapper<T: Any> {

    /**
     * 엔티티의 정보를 읽어 DynamoDB Item 정보 형식인 `Map<String, AttributeValue>` 로 변환합니다.
     *
     * @param entity 변환할 entity
     * @return DynamoDB Item 정보
     */
    fun mapToDynamoItem(item: T): Map<String, AttributeValue>
}

/**
 * Iterable의 엔티티들을 DynamoDB WriteRequest로 변환합니다.
 *
 * ```
 * val writeRequests = items.buildWriteRequest(mapper)
 * ```
 */
fun <T: Any> Iterable<T>.buildWriteRequest(mapper: DynamoItemMapper<T>): List<WriteRequest> {
    return this
        .map {
            val item = mapper.mapToDynamoItem(it)
            WriteRequest.builder()
                .putRequest { builder -> builder.item(item) }
                .build()
        }
}
