@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.aws.dynamodb.enhanced

import io.bluetape4k.aws.dynamodb.model.keyOf
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional

/**
 * Key로 아이템을 조회합니다.
 *
 * @param partitionValue 파티션 키 값
 * @param sortValue 정렬 키 값 (옵션)
 * @return 조회된 아이템 또는 null
 */
inline fun <T: Any> DynamoDbTable<T>.getItem(
    partitionValue: Any,
    sortValue: Any? = null,
): T? =
    getItem(keyOf(partitionValue, sortValue))

/**
 * 아이템을 삭제합니다.
 *
 * @param partitionValue 파티션 키 값
 * @param sortValue 정렬 키 값 (옵션)
 * @return 삭제된 아이템
 */
inline fun <T: Any> DynamoDbTable<T>.deleteItem(
    partitionValue: Any,
    sortValue: Any? = null,
): T? {
    val key =
        when (sortValue) {
            null -> {
                Key.builder().partitionValue(partitionValue.toString()).build()
            }

            else -> {
                Key
                    .builder()
                    .partitionValue(partitionValue.toString())
                    .sortValue(sortValue.toString())
                    .build()
            }
        }
    return deleteItem(key)
}

/**
 * 모든 아이템을 조회합니다 (스캔).
 *
 * @return 모든 아이템 리스트
 */
fun <T> DynamoDbTable<T>.findAll(): List<T> = scan().items().toList()

/**
 * 특정 파티션의 모든 아이템을 조회합니다.
 *
 * @param partitionValue 파티션 키 값
 * @return 아이템 리스트
 */
fun <T: Any> DynamoDbTable<T>.findByPartition(partitionValue: String): List<T> {
    val key = Key.builder().partitionValue(partitionValue).build()
    return query(QueryConditional.keyEqualTo(key)).items().toList()
}

/**
 * 아이템이 존재하는지 확인합니다.
 *
 * @param partitionValue 파티션 키 값
 * @param sortValue 정렬 키 값 (옵션)
 * @return 존재하면 true
 */
inline fun <T: Any> DynamoDbTable<T>.exists(
    partitionValue: Any,
    sortValue: Any? = null,
): Boolean = getItem(partitionValue, sortValue) != null

/**
 * 결과를 List로 변환합니다.
 *
 * @return 모든 아이템 리스트
 */
fun <T> PageIterable<T>.toList(): List<T> = items().toList()
