package io.bluetape4k.aws.dynamodb.repository

import io.bluetape4k.aws.dynamodb.enhanced.batchWriteItems
import io.bluetape4k.aws.dynamodb.model.DynamoDbEntity
import io.bluetape4k.aws.dynamodb.model.dynamoDbKeyOf
import io.bluetape4k.coroutines.flow.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.future.await
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.model.BatchWriteResult
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest

/**
 * Coroutines 환경에서 DynamoDB Repository를 사용하기 위한 인터페이스
 */
interface DynamoDbCoroutineRepository<T: DynamoDbEntity> {

    val client: DynamoDbEnhancedAsyncClient
    val table: DynamoDbAsyncTable<T>
    val itemClass: Class<T>

    suspend fun findByKey(key: Key): T? {
        return table.getItem(key).await()
    }

    suspend fun findFirst(request: QueryEnhancedRequest): Flow<T> {
        return table.query(request).findFirst()
    }

    suspend fun findFirstByPartitionKey(partitionKey: String): Flow<T> {
        val request = QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.keyEqualTo(dynamoDbKeyOf(partitionKey)))
            .build()
        return findFirst(request)
    }

    suspend fun count(request: QueryEnhancedRequest): Long {
        return table.query(request).count()
    }

    suspend fun save(item: T) {
        table.putItem(item).await()
    }

    fun saveAll(items: Collection<T>): Flow<BatchWriteResult> {
        return client.batchWriteItems(itemClass, table, items = items)
    }

    suspend fun update(item: T): T? {
        return table.updateItem(item).await()
    }

    suspend fun delete(item: T): T? {
        return table.deleteItem(item.key).await()
    }

    suspend fun delete(key: Key): T? {
        return table.deleteItem(key).await()
    }

    fun deleteAll(items: Iterable<T>): Flow<T> {
        return items.asFlow()
            .async { item ->
                delete(item)
            }
            .mapNotNull { it }
//        return items.asFlow()
//            .flatMapMerge { item ->
//                flow { emit(delete(item)) }   // flowOf 를 사용하면 병렬로 수행하지 안됩니다.
//            }
//            .mapNotNull { it }
    }

    fun deleteAllByKeys(keys: Iterable<Key>): Flow<T> {
        return keys.asFlow()
            .async { key ->
                delete(key)
            }
            .mapNotNull { it }
//        return keys.asFlow()
//            .flatMapMerge { key ->
//                flow { emit(delete(key)) }   // flowOf 를 사용하면 병렬로 수행하지 안됩니다.
//            }
//            .mapNotNull { it }
    }
}
