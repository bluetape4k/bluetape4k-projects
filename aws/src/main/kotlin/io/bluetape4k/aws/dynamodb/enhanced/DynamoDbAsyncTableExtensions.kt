package io.bluetape4k.aws.dynamodb.enhanced

import io.bluetape4k.aws.dynamodb.model.keyOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.asFlow
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest

/**
 * Key로 아이템을 조회합니다.
 *
 * @param partitionValue 파티션 키 값
 * @param sortValue 정렬 키 값 (옵션)
 * @return 조회된 아이템 또는 null
 */
suspend inline fun <T: Any> DynamoDbAsyncTable<T>.getItem(
    partitionValue: Any,
    sortValue: Any? = null,
): T? = getItem(keyOf(partitionValue, sortValue)).await()


/**
 * 아이템을 저장합니다.
 *
 * @param item 저장할 아이템
 */
suspend inline fun <T: Any> DynamoDbAsyncTable<T>.putItem(item: T) {
    putItem(item).await()
}

/**
 * 아이템을 저장합니다.
 *
 * @param item 저장할 아이템
 * @param builder PutItemEnhancedRequest 빌더
 */
suspend inline fun <reified T: Any> DynamoDbAsyncTable<T>.putItem(
    item: T,
    @BuilderInference builder: PutItemEnhancedRequest.Builder<T>.() -> Unit,
) {
    val request =
        PutItemEnhancedRequest
            .builder(T::class.java)
            .item(item)
            .apply(builder)
            .build()
    putItem(request).await()
}

/**
 * 아이템을 삭제합니다.
 *
 * @param partitionValue 파티션 키 값
 * @param sortValue 정렬 키 값 (옵션)
 * @return 삭제된 아이템
 */
suspend inline fun <T: Any> DynamoDbAsyncTable<T>.deleteItem(
    partitionValue: Any,
    sortValue: Any? = null,
): T? = deleteItem(keyOf(partitionValue, sortValue)).await()

/**
 * 테이블을 전체 스캔합니다.
 *
 * @param builder ScanEnhancedRequest 빌더
 * @return 스캔 결과 리스트
 */
inline fun <T: Any> DynamoDbAsyncTable<T>.scanAll(
    @BuilderInference builder: ScanEnhancedRequest.Builder.() -> Unit = {},
): Flow<T> {
    val request = ScanEnhancedRequest.builder().apply(builder).build()
    return scan(request).items().asFlow()
}

/**
 * 테이블을 쿼리합니다.
 *
 * @param queryConditional 쿼리 조건
 * @param builder QueryEnhancedRequest 빌더
 * @return 쿼리 결과 리스트
 */
inline fun <T: Any> DynamoDbAsyncTable<T>.queryAll(
    queryConditional: QueryConditional,
    @BuilderInference builder: QueryEnhancedRequest.Builder.() -> Unit = {},
): Flow<T> {
    val request =
        QueryEnhancedRequest
            .builder()
            .queryConditional(queryConditional)
            .apply(builder)
            .build()

    return query(request).items().asFlow()
}

/**
 * 파티션 키로 쿼리합니다.
 *
 * @param partitionValue 파티션 키 값
 * @param builder QueryEnhancedRequest 빌더
 * @return 쿼리 결과 리스트
 */
inline fun <T: Any> DynamoDbAsyncTable<T>.queryByPartition(
    partitionValue: String,
    @BuilderInference builder: QueryEnhancedRequest.Builder.() -> Unit = {},
): Flow<T> {
    val key = keyOf(partitionValue)
    return queryAll(QueryConditional.keyEqualTo(key), builder)
}

/**
 * 모든 아이템을 조회합니다 (스캔).
 *
 * @return 모든 아이템 리스트
 */
fun <T: Any> DynamoDbAsyncTable<T>.findAll(): Flow<T> = scanAll()

/**
 * 특정 파티션의 모든 아이템을 조회합니다.
 *
 * @param partitionValue 파티션 키 값
 * @return 아이템 리스트
 */
fun <T: Any> DynamoDbAsyncTable<T>.findByPartition(partitionValue: String): Flow<T> =
    queryByPartition(partitionValue)

/**
 * 아이템이 존재하는지 확인합니다.
 *
 * @param partitionValue 파티션 키 값
 * @param sortValue 정렬 키 값 (옵션)
 * @return 존재하면 true
 */
suspend inline fun <T: Any> DynamoDbAsyncTable<T>.exists(
    partitionValue: Any,
    sortValue: Any? = null,
): Boolean = getItem(partitionValue, sortValue) != null
