package io.bluetape4k.aws.dynamodb.repository

import io.bluetape4k.aws.dynamodb.enhanced.batchWriteItems
import io.bluetape4k.aws.dynamodb.model.DynamoDbEntity
import io.bluetape4k.aws.dynamodb.model.keyOf
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
 * Coroutines 환경에서 DynamoDB Repository를 사용하기 위한 인터페이스입니다.
 *
 * 구현체는 [table], [client], [itemClass]를 제공하면 기본 CRUD 동작을 재사용할 수 있습니다.
 *
 * ```kotlin
 * val saved = repository.update(entity)
 * check(saved?.key == entity.key)
 * ```
 */
interface DynamoDbCoroutineRepository<T: DynamoDbEntity> {

    /** Repository가 사용하는 Enhanced Async Client 입니다. */
    val client: DynamoDbEnhancedAsyncClient

    /** CRUD 대상 테이블입니다. */
    val table: DynamoDbAsyncTable<T>

    /** 배치 쓰기 시 사용하는 엔티티 클래스입니다. */
    val itemClass: Class<T>

    /**
     * [Key]로 단건을 조회합니다.
     *
     * ```kotlin
     * val found = repository.findByKey(key)
     * check(found == null || found.key == key)
     * ```
     */
    suspend fun findByKey(key: Key): T? {
        return table.getItem(key).await()
    }

    /**
     * [QueryEnhancedRequest] 기준으로 첫 페이지 아이템을 조회합니다.
     *
     * 반환 목록은 비어 있을 수 있으며, 테스트에서 첫 페이지의 item 리스트를 그대로 반환하는 동작을 검증합니다.
     */
    suspend fun findFirst(request: QueryEnhancedRequest): List<T> {
        return table.query(request).findFirst()
    }

    /**
     * 파티션 키 값으로 첫 페이지를 조회합니다.
     *
     * ```kotlin
     * val items = repository.findFirstByPartitionKey("customer#1")
     * check(items is List<*>)
     * ```
     */
    suspend fun findFirstByPartitionKey(partitionKey: String): List<T> {
        val request = QueryEnhancedRequest.builder()
            .queryConditional(QueryConditional.keyEqualTo(keyOf(partitionKey)))
            .build()
        return findFirst(request)
    }

    /**
     * [QueryEnhancedRequest] 결과의 전체 아이템 개수를 계산합니다.
     */
    suspend fun count(request: QueryEnhancedRequest): Long {
        return table.query(request).count()
    }

    /**
     * 엔티티를 저장합니다.
     */
    suspend fun save(item: T) {
        table.putItem(item).await()
    }

    /**
     * 여러 엔티티를 배치로 저장하고 각 배치 결과를 [Flow]로 반환합니다.
     *
     * ```kotlin
     * val count = repository.saveAll(items).count()
     * check(count >= 1)
     * ```
     */
    fun saveAll(items: Collection<T>): Flow<BatchWriteResult> {
        return client.batchWriteItems(itemClass, table, items = items)
    }

    /**
     * 엔티티를 업데이트하고 DynamoDB가 반환한 최신 엔티티를 반환합니다.
     */
    suspend fun update(item: T): T? {
        return table.updateItem(item).await()
    }

    /**
     * 엔티티의 [DynamoDbEntity.key]를 사용해 삭제합니다.
     */
    suspend fun delete(item: T): T? {
        return table.deleteItem(item.key).await()
    }

    /**
     * [Key]로 엔티티를 삭제합니다.
     */
    suspend fun delete(key: Key): T? {
        return table.deleteItem(key).await()
    }

    /**
     * 엔티티 목록을 비동기로 순회하며 삭제 성공 항목만 [Flow]로 반환합니다.
     *
     * `null` 반환(삭제 대상 미존재)은 [mapNotNull]로 제거됩니다.
     */
    fun deleteAll(items: Iterable<T>): Flow<T> {
        return items.asFlow()
            .async { item ->
                delete(item)
            }
            .mapNotNull { it }
    }

    /**
     * 키 목록을 비동기로 순회하며 삭제 성공 항목만 [Flow]로 반환합니다.
     */
    fun deleteAllByKeys(keys: Iterable<Key>): Flow<T> {
        return keys.asFlow()
            .async { key ->
                delete(key)
            }
            .mapNotNull { it }
    }
}
