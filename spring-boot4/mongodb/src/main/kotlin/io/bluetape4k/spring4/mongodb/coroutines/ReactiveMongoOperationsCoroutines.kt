package io.bluetape4k.spring4.mongodb.coroutines

import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.CollectionOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.aggregate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.TypedAggregation
import org.springframework.data.mongodb.core.count
import org.springframework.data.mongodb.core.createCollection
import org.springframework.data.mongodb.core.dropCollection
import org.springframework.data.mongodb.core.exists
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.mongodb.core.findAndRemove
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.UpdateDefinition
import org.springframework.data.mongodb.core.remove
import org.springframework.data.mongodb.core.tail
import org.springframework.data.mongodb.core.updateFirst
import org.springframework.data.mongodb.core.updateMulti
import org.springframework.data.mongodb.core.upsert

// ====================================================
// 조회 - Flow (다건)
// ====================================================

/**
 * [Query] 조건에 맞는 문서를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `find(query, T::class.java).asFlow()`를 호출합니다.
 * - 조건에 맞는 결과가 없으면 빈 [Flow]가 반환됩니다.
 *
 * ```kotlin
 * val users: Flow<User> = mongoOps.findAsFlow(Query(Criteria.where("age").gt(20)))
 * users.collect { println(it) }
 * ```
 */
inline fun <reified T: Any> ReactiveMongoOperations.findAsFlow(query: Query): Flow<T> = find<T>(query).asFlow()

/**
 * [Query] 조건에 맞는 문서를 지정한 컬렉션에서 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `find(query, T::class.java, collectionName).asFlow()`를 호출합니다.
 *
 * ```kotlin
 * val users: Flow<User> = mongoOps.findAsFlow(query, "users_archive")
 * ```
 */
inline fun <reified T: Any> ReactiveMongoOperations.findAsFlow(
    query: Query,
    collectionName: String,
): Flow<T> = find<T>(query, collectionName).asFlow()

/**
 * 컬렉션의 모든 문서를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `findAll(T::class.java).asFlow()`를 호출합니다.
 *
 * ```kotlin
 * val allUsers: Flow<User> = mongoOps.findAllAsFlow<User>()
 * ```
 */
inline fun <reified T: Any> ReactiveMongoOperations.findAllAsFlow(): Flow<T> = findAll<T>().asFlow()

// ====================================================
// 조회 - Suspend (단건)
// ====================================================

/**
 * [Query] 조건에 맞는 첫 번째 문서를 반환합니다.
 *
 * ## 동작/계약
 * - 결과가 없으면 null을 반환합니다.
 *
 * ```kotlin
 * val user: User? = mongoOps.findOneOrNullSuspending(Query(Criteria.where("name").`is`("Alice")))
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.findOneOrNullSuspending(query: Query): T? =
    findOne<T>(query).awaitSingleOrNull()

/**
 * [Query] 조건에 맞는 첫 번째 문서를 반환합니다.
 *
 * ## 동작/계약
 * - 결과가 없으면 [NoSuchElementException]이 발생합니다.
 *
 * ```kotlin
 * val user: User = mongoOps.findOneSuspending(Query(Criteria.where("name").`is`("Alice")))
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.findOneSuspending(query: Query): T =
    findOne<T>(query).awaitSingle()

/**
 * ID로 문서를 조회합니다.
 *
 * ## 동작/계약
 * - 결과가 없으면 null을 반환합니다.
 *
 * ```kotlin
 * val user: User? = mongoOps.findByIdOrNullSuspending("userId123")
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.findByIdOrNullSuspending(id: Any): T? =
    findById<T>(id).awaitSingleOrNull()

/**
 * ID로 문서를 조회합니다.
 *
 * ## 동작/계약
 * - 결과가 없으면 [NoSuchElementException]이 발생합니다.
 *
 * ```kotlin
 * val user: User = mongoOps.findByIdSuspending("userId123")
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.findByIdSuspending(id: Any): T =
    findById<T>(id).awaitSingle()

// ====================================================
// 집계 함수
// ====================================================

/**
 * [Query] 조건에 맞는 문서 수를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `count(query, T::class.java).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val count: Long = mongoOps.countSuspending<User>(Query(Criteria.where("age").gt(20)))
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.countSuspending(query: Query = Query()): Long =
    count<T>(query).awaitSingle()

/**
 * [Query] 조건에 맞는 문서가 존재하는지 확인합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `exists(query, T::class.java).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val exists: Boolean = mongoOps.existsSuspending<User>(Query(Criteria.where("email").`is`("alice@example.com")))
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.existsSuspending(query: Query): Boolean =
    exists<T>(query).awaitSingle()

// ====================================================
// 쓰기 - Insert/Save
// ====================================================

/**
 * 엔티티를 삽입하고 저장된 엔티티를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `insert(entity).awaitSingle()`을 호출합니다.
 * - 동일 ID 문서가 이미 존재하면 예외가 발생합니다. (upsert는 [upsertSuspending] 사용)
 *
 * ```kotlin
 * val saved: User = mongoOps.insertSuspending(User(name = "Alice", age = 30))
 * ```
 */
suspend fun <T: Any> ReactiveMongoOperations.insertSuspending(entity: T): T = insert(entity).awaitSingle()

/**
 * 여러 엔티티를 삽입하고 저장된 엔티티를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `insertAll(entities).asFlow()`를 호출합니다.
 *
 * ```kotlin
 * val savedUsers: Flow<User> = mongoOps.insertAllAsFlow(listOf(user1, user2))
 * ```
 */
fun <T: Any> ReactiveMongoOperations.insertAllAsFlow(entities: Collection<T>): Flow<T> = insertAll(entities).asFlow()

/**
 * 엔티티를 저장합니다 (삽입 또는 업데이트).
 *
 * ## 동작/계약
 * - ID가 없으면 삽입, 있으면 전체 교체(replace)합니다.
 *
 * ```kotlin
 * val saved: User = mongoOps.saveSuspending(user)
 * ```
 */
suspend fun <T: Any> ReactiveMongoOperations.saveSuspending(entity: T): T = save(entity).awaitSingle()

// ====================================================
// 쓰기 - Update/Upsert
// ====================================================

/**
 * [Query] 조건에 맞는 첫 번째 문서를 업데이트합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `updateFirst(query, update, T::class.java).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val result: UpdateResult = mongoOps.updateFirstSuspending<User>(
 *     Query(Criteria.where("name").`is`("Alice")),
 *     Update().set("age", 31)
 * )
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.updateFirstSuspending(
    query: Query,
    update: UpdateDefinition,
): UpdateResult = updateFirst<T>(query, update).awaitSingle()

/**
 * [Query] 조건에 맞는 모든 문서를 업데이트합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `updateMulti(query, update, T::class.java).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val result: UpdateResult = mongoOps.updateMultiSuspending<User>(
 *     Query(Criteria.where("city").`is`("Seoul")),
 *     Update().inc("score", 10)
 * )
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.updateMultiSuspending(
    query: Query,
    update: UpdateDefinition,
): UpdateResult = updateMulti<T>(query, update).awaitSingle()

/**
 * [Query] 조건에 맞는 문서를 업데이트하거나, 없으면 삽입합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `upsert(query, update, T::class.java).awaitSingle()`을 호출합니다.
 * - 문서가 없을 경우 `update`의 `$setOnInsert` 필드가 적용됩니다.
 *
 * ```kotlin
 * val result: UpdateResult = mongoOps.upsertSuspending<User>(
 *     Query(Criteria.where("name").`is`("Alice")),
 *     Update().set("age", 31)
 * )
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.upsertSuspending(
    query: Query,
    update: UpdateDefinition,
): UpdateResult = upsert<T>(query, update).awaitSingle()

// ====================================================
// 쓰기 - Remove
// ====================================================

/**
 * [Query] 조건에 맞는 문서를 삭제합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `remove(query, T::class.java).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val result: DeleteResult = mongoOps.removeSuspending<User>(
 *     Query(Criteria.where("name").`is`("Alice"))
 * )
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.removeSuspending(query: Query): DeleteResult =
    remove<T>(query).awaitSingle()

/**
 * 엔티티를 삭제합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `remove(entity).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val result: DeleteResult = mongoOps.removeSuspending(user)
 * ```
 */
suspend fun <T: Any> ReactiveMongoOperations.removeSuspending(entity: T): DeleteResult = remove(entity).awaitSingle()

// ====================================================
// 조회 + 수정/삭제 (Atomic)
// ====================================================

/**
 * [Query] 조건에 맞는 첫 번째 문서를 원자적으로 수정하고 수정 전/후 문서를 반환합니다.
 *
 * ## 동작/계약
 * - 기본적으로 수정 전 문서를 반환합니다. 수정 후 반환이 필요하면 `FindAndModifyOptions` 사용.
 * - 결과가 없으면 null을 반환합니다.
 *
 * ```kotlin
 * val old: User? = mongoOps.findAndModifySuspending<User>(
 *     Query(Criteria.where("name").`is`("Alice")),
 *     Update().set("age", 31)
 * )
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.findAndModifySuspending(
    query: Query,
    update: UpdateDefinition,
): T? = findAndModify(query, update, T::class.java).awaitSingleOrNull()

/**
 * [Query] 조건에 맞는 첫 번째 문서를 원자적으로 삭제하고 삭제된 문서를 반환합니다.
 *
 * ## 동작/계약
 * - 결과가 없으면 null을 반환합니다.
 *
 * ```kotlin
 * val removed: User? = mongoOps.findAndRemoveSuspending<User>(
 *     Query(Criteria.where("name").`is`("Alice"))
 * )
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.findAndRemoveSuspending(query: Query): T? =
    findAndRemove<T>(query).awaitSingleOrNull()

// ====================================================
// Distinct 조회
// ====================================================

/**
 * [Query] 조건에 맞는 특정 필드의 고유 값을 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `findDistinct(query, field, I::class.java, O::class.java).asFlow()`를 호출합니다.
 *
 * ```kotlin
 * val cities: Flow<String> = mongoOps.findDistinctAsFlow<User, String>(
 *     query = Query(),
 *     field = "city"
 * )
 * ```
 */
inline fun <reified I: Any, reified O: Any> ReactiveMongoOperations.findDistinctAsFlow(
    query: Query,
    field: String,
): Flow<O> = findDistinct(query, field, I::class.java, O::class.java).asFlow()

// ====================================================
// Aggregation
// ====================================================

/**
 * Aggregation Pipeline을 실행하고 결과를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `aggregate(aggregation, I::class.java, O::class.java).asFlow()`를 호출합니다.
 *
 * ```kotlin
 * val aggregation = Aggregation.newAggregation(
 *     Aggregation.match(Criteria.where("age").gt(20)),
 *     Aggregation.group("city").count().`as`("count")
 * )
 * val results: Flow<CityCount> = mongoOps.aggregateAsFlow<User, CityCount>(aggregation)
 * ```
 */
inline fun <reified I: Any, reified O: Any> ReactiveMongoOperations.aggregateAsFlow(
    aggregation: Aggregation,
): Flow<O> = aggregate(aggregation, I::class.java, O::class.java).asFlow()

/**
 * TypedAggregation Pipeline을 실행하고 결과를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `aggregate(aggregation, O::class.java).asFlow()`를 호출합니다.
 *
 * ```kotlin
 * val typedAgg = Aggregation.newAggregation(User::class.java,
 *     Aggregation.group("city").count().`as`("count")
 * )
 * val results: Flow<CityCount> = mongoOps.aggregateAsFlow<CityCount>(typedAgg)
 * ```
 */
inline fun <reified O: Any> ReactiveMongoOperations.aggregateAsFlow(aggregation: TypedAggregation<*>): Flow<O> =
    aggregate<O>(aggregation).asFlow()

// ====================================================
// Tailable Cursor
// ====================================================

/**
 * Tailable cursor를 사용하여 컬렉션에 새로 추가되는 문서를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - Tailable cursor는 Capped Collection에서만 동작합니다.
 * - 내부적으로 `tail(query, T::class.java).asFlow()`를 호출합니다.
 * - 컬렉션에 문서가 추가될 때마다 Flow가 방출합니다.
 *
 * ```kotlin
 * val events: Flow<Event> = mongoOps.tailAsFlow<Event>(Query())
 * events.collect { event -> processEvent(event) }
 * ```
 */
inline fun <reified T: Any> ReactiveMongoOperations.tailAsFlow(query: Query): Flow<T> = tail<T>(query).asFlow()

// ====================================================
// 컬렉션 관리
// ====================================================

/**
 * 컬렉션 존재 여부를 확인합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `collectionExists(collectionName).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val exists: Boolean = mongoOps.collectionExistsSuspending("users")
 * ```
 */
suspend fun ReactiveMongoOperations.collectionExistsSuspending(collectionName: String): Boolean =
    collectionExists(collectionName).awaitSingle()

/**
 * 컬렉션을 삭제합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `dropCollection(T::class.java).awaitSingleOrNull()`을 호출합니다.
 * - 컬렉션이 없으면 무시됩니다.
 *
 * ```kotlin
 * mongoOps.dropCollectionSuspending<User>()
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.dropCollectionSuspending() {
    dropCollection<T>().awaitSingleOrNull()
}

/**
 * 컬렉션을 이름으로 삭제합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `dropCollection(collectionName).awaitSingleOrNull()`을 호출합니다.
 *
 * ```kotlin
 * mongoOps.dropCollectionSuspending("users")
 * ```
 */
suspend fun ReactiveMongoOperations.dropCollectionSuspending(collectionName: String) {
    dropCollection(collectionName).awaitSingleOrNull()
}

/**
 * 컬렉션을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `createCollection(T::class.java).awaitSingle()`을 호출합니다.
 * - 이미 존재하는 컬렉션이면 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * mongoOps.createCollectionSuspending<User>()
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.createCollectionSuspending() {
    createCollection<T>().awaitSingleOrNull()
}

/**
 * [CollectionOptions]를 지정하여 컬렉션을 생성합니다.
 *
 * ## 동작/계약
 * - Capped Collection, JSON Schema 검증 등 옵션을 지정할 수 있습니다.
 *
 * ```kotlin
 * // Capped Collection 생성
 * mongoOps.createCollectionSuspending<Event>(
 *     CollectionOptions.empty().capped().size(1024 * 1024).maxDocuments(1000)
 * )
 * ```
 */
suspend inline fun <reified T: Any> ReactiveMongoOperations.createCollectionSuspending(options: CollectionOptions) {
    createCollection<T>(options).awaitSingleOrNull()
}
