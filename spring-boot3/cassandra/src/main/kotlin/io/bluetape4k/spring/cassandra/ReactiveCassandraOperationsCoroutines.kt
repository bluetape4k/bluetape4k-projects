package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.cql.Statement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.cassandra.ReactiveResultSet
import org.springframework.data.cassandra.core.DeleteOptions
import org.springframework.data.cassandra.core.EntityWriteResult
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.data.cassandra.core.WriteResult
import org.springframework.data.cassandra.core.count
import org.springframework.data.cassandra.core.cql.QueryOptions
import org.springframework.data.cassandra.core.delete
import org.springframework.data.cassandra.core.deleteById
import org.springframework.data.cassandra.core.exists
import org.springframework.data.cassandra.core.query
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.cassandra.core.select
import org.springframework.data.cassandra.core.selectOne
import org.springframework.data.cassandra.core.selectOneById
import org.springframework.data.cassandra.core.truncate
import org.springframework.data.cassandra.core.update
import org.springframework.data.domain.Slice

/**
 * [Statement] 조회 결과를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `select<T>(statement).asFlow()`를 호출합니다.
 * - 결과가 없으면 빈 Flow가 반환됩니다.
 *
 * ```kotlin
 * val rows = reactiveOps.selectAsFlow<User>(statement)
 * // result == rows
 * ```
 */
inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(statement: Statement<*>): Flow<T> =
    select<T>(statement).asFlow()

/**
 * CQL 문자열 조회 결과를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `select<T>(cql).asFlow()`를 호출합니다.
 * - 매핑 실패 예외는 수집 시점에 그대로 전파됩니다.
 *
 * ```kotlin
 * val rows = reactiveOps.selectAsFlow<User>("SELECT * FROM users")
 * // result == rows
 * ```
 */
inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(cql: String): Flow<T> = select<T>(cql).asFlow()

/**
 * [Query] 조회 결과를 [Flow]로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `select<T>(query).asFlow()`를 호출합니다.
 * - 조건에 맞는 결과가 없으면 빈 Flow가 반환됩니다.
 *
 * ```kotlin
 * val rows = reactiveOps.selectAsFlow<User>(query)
 * // result == rows
 * ```
 */
inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(query: Query): Flow<T> = select<T>(query).asFlow()

/**
 * [Statement]로 단건을 조회합니다.
 *
 * ## 동작/계약
 * - `selectOne<T>(statement).awaitSingle()`을 호출합니다.
 * - 결과가 없으면 예외가 전파됩니다.
 *
 * ```kotlin
 * val user = reactiveOps.selectOneSuspending<User>(statement)
 * // user.id != null
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneSuspending(statement: Statement<*>): T =
    selectOne<T>(statement).awaitSingle()

/**
 * [Statement]로 단건을 조회하고 없으면 null을 반환합니다.
 *
 * ## 동작/계약
 * - `selectOne<T>(statement).awaitSingleOrNull()`을 호출합니다.
 * - 결과가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val user = reactiveOps.selectOneOrNullSuspending<User>(statement)
 * // user == null || user is User
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneOrNullSuspending(
    statement: Statement<*>,
): T? = selectOne<T>(statement).awaitSingleOrNull()

/**
 * CQL 문자열로 단건을 조회합니다.
 *
 * ## 동작/계약
 * - `selectOne<T>(cql).awaitSingle()`을 호출합니다.
 * - 결과가 없으면 예외가 전파됩니다.
 *
 * ```kotlin
 * val user = reactiveOps.selectOneSuspending<User>("SELECT * FROM users WHERE id = 1")
 * // user.id != null
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneSuspending(cql: String): T =
    selectOne<T>(cql).awaitSingle()

/**
 * CQL 문자열로 단건을 조회하고 없으면 null을 반환합니다.
 *
 * ## 동작/계약
 * - `selectOne<T>(cql).awaitSingleOrNull()`을 호출합니다.
 * - 결과가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val user = reactiveOps.selectOneOrNullSuspending<User>("SELECT * FROM users WHERE id = 999")
 * // user == null
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneOrNullSuspending(cql: String): T? =
    selectOne<T>(cql).awaitSingleOrNull()

/**
 * [Query]로 단건을 조회합니다.
 *
 * ## 동작/계약
 * - `selectOne<T>(query).awaitSingle()`을 호출합니다.
 * - 결과가 없으면 예외가 전파됩니다.
 *
 * ```kotlin
 * val user = reactiveOps.selectOneSuspending<User>(query)
 * // user.id != null
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneSuspending(query: Query): T =
    selectOne<T>(query).awaitSingle()

/**
 * [Query]로 단건을 조회하고 없으면 null을 반환합니다.
 *
 * ## 동작/계약
 * - `selectOne<T>(query).awaitSingleOrNull()`을 호출합니다.
 * - 결과가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val user = reactiveOps.selectOneOrNullSuspending<User>(query)
 * // user == null || user is User
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneOrNullSuspending(query: Query): T? =
    selectOne<T>(query).awaitSingleOrNull()

/**
 * [Statement]로 [Slice]를 조회합니다.
 *
 * ## 동작/계약
 * - `slice(statement, T::class.java).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val slice = reactiveOps.sliceSuspending<User>(statement)
 * // slice.content.size >= 0
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.sliceSuspending(statement: Statement<*>): Slice<T> =
    slice(statement, T::class.java).awaitSingle()

/**
 * [Query]로 [Slice]를 조회합니다.
 *
 * ## 동작/계약
 * - `slice(query, T::class.java).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val slice = reactiveOps.sliceSuspending<User>(query)
 * // slice.content.size >= 0
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.sliceSuspending(query: Query): Slice<T> =
    slice(query, T::class.java).awaitSingle()

/**
 * [Statement]를 실행하고 [ReactiveResultSet]을 반환합니다.
 *
 * ## 동작/계약
 * - `execute(statement).awaitSingle()`을 호출합니다.
 * - 실행 실패 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val resultSet = reactiveOps.executeSuspending(statement)
 * // resultSet != null
 * ```
 */
suspend fun ReactiveCassandraOperations.executeSuspending(statement: Statement<*>): ReactiveResultSet =
    execute(statement).awaitSingle()

/**
 * [Query]와 [Update]로 갱신하고 성공 여부를 반환합니다.
 *
 * ## 동작/계약
 * - `update<T>(query, update).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val updated = reactiveOps.updateSuspending<User>(query, update)
 * // updated == true
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.updateSuspending(
    query: Query,
    update: Update,
): Boolean = update<T>(query, update).awaitSingle()

/**
 * [Query]로 삭제하고 성공 여부를 반환합니다.
 *
 * ## 동작/계약
 * - `delete<T>(query).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val deleted = reactiveOps.deleteSuspending<User>(query)
 * // deleted == true
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.deleteSuspending(query: Query): Boolean =
    delete<T>(query).awaitSingle()

/**
 * 전체 건수를 반환합니다.
 *
 * ## 동작/계약
 * - `count<T>().awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val count = reactiveOps.countSuspending<User>()
 * // count >= 0
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.countSuspending(): Long = count<T>().awaitSingle()

/**
 * [Query] 조건의 건수를 반환합니다.
 *
 * ## 동작/계약
 * - `count<T>(query).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val count = reactiveOps.countSuspending<User>(query)
 * // count >= 0
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.countSuspending(query: Query): Long =
    count<T>(query).awaitSingle()

/**
 * 데이터 존재 여부를 반환합니다.
 *
 * ## 동작/계약
 * - `query<T>().exists().awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val exists = reactiveOps.existsSuspending<User>()
 * // exists == true || exists == false
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.existsSuspending(): Boolean =
    query<T>().exists().awaitSingle()

/**
 * [Query] 조건으로 존재 여부를 반환합니다.
 *
 * ## 동작/계약
 * - `exists<T>(query).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val exists = reactiveOps.existsSuspending<User>(query)
 * // exists == true || exists == false
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.existsSuspending(query: Query): Boolean =
    exists<T>(query).awaitSingle()

/**
 * id 기준으로 존재 여부를 반환합니다.
 *
 * ## 동작/계약
 * - `exists<T>(id).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val exists = reactiveOps.existsSuspending<User>("user-1")
 * // exists == true || exists == false
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.existsSuspending(id: Any): Boolean =
    exists<T>(id).awaitSingle()

/**
 * id 기준으로 단건을 조회합니다.
 *
 * ## 동작/계약
 * - `selectOneById<T>(id).awaitSingle()`을 호출합니다.
 * - 결과가 없으면 예외가 전파됩니다.
 *
 * ```kotlin
 * val user = reactiveOps.selectOneByIdSuspending<User>("user-1")
 * // user.id != null
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneByIdSuspending(id: Any): T =
    selectOneById<T>(id).awaitSingle()

/**
 * id 기준으로 단건을 조회하고 없으면 null을 반환합니다.
 *
 * ## 동작/계약
 * - `selectOneById<T>(id).awaitSingleOrNull()`을 호출합니다.
 * - 결과가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val user = reactiveOps.selectOneOrNullByIdSuspending<User>("user-999")
 * // user == null
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.selectOneOrNullByIdSuspending(id: Any): T? =
    selectOneById<T>(id).awaitSingleOrNull()

/**
 * 엔티티를 저장하고 저장된 엔티티를 반환합니다.
 *
 * ## 동작/계약
 * - `insert(entity).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val saved = reactiveOps.insertSuspending(user)
 * // saved.id != null
 * ```
 */
suspend fun <T: Any> ReactiveCassandraOperations.insertSuspending(entity: T): T = insert(entity).awaitSingle()

/**
 * 엔티티를 저장하고 [InsertOptions]를 적용합니다.
 *
 * ## 동작/계약
 * - `insert(entity, options).awaitSingle()`을 호출합니다.
 * - LWT 결과는 [EntityWriteResult]에서 확인합니다.
 *
 * ```kotlin
 * val result = reactiveOps.insertSuspending(user, insertOptions { withIfNotExists() })
 * // result.wasApplied() == true
 * ```
 */
suspend fun <T: Any> ReactiveCassandraOperations.insertSuspending(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> = insert(entity, options).awaitSingle()

/**
 * 엔티티를 갱신하고 갱신된 엔티티를 반환합니다.
 *
 * ## 동작/계약
 * - `update(entity).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val updated = reactiveOps.updateSuspending(user)
 * // updated.id != null
 * ```
 */
suspend fun <T: Any> ReactiveCassandraOperations.updateSuspending(entity: T): T = update(entity).awaitSingle()

/**
 * 엔티티를 갱신하고 [UpdateOptions]를 적용합니다.
 *
 * ## 동작/계약
 * - `update(entity, options).awaitSingle()`을 호출합니다.
 * - LWT 결과는 [EntityWriteResult]에서 확인합니다.
 *
 * ```kotlin
 * val result = reactiveOps.updateSuspending(user, updateOptions { timeout(500) })
 * // result.wasApplied() == true
 * ```
 */
suspend fun <T: Any> ReactiveCassandraOperations.updateSuspending(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> = update(entity, options).awaitSingle()

/**
 * 엔티티를 삭제하고 삭제된 엔티티를 반환합니다.
 *
 * ## 동작/계약
 * - `delete(entity).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val deleted = reactiveOps.deleteSuspending(user)
 * // deleted.id != null
 * ```
 */
suspend fun <T: Any> ReactiveCassandraOperations.deleteSuspending(entity: T): T = delete(entity).awaitSingle()

/**
 * 엔티티를 삭제하고 [QueryOptions]를 적용합니다.
 *
 * ## 동작/계약
 * - `delete(entity, options).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val result = reactiveOps.deleteSuspending(user, queryOptions { pageSize(1) })
 * // result.wasApplied() == true
 * ```
 */
suspend fun <T: Any> ReactiveCassandraOperations.deleteSuspending(
    entity: T,
    options: QueryOptions,
): WriteResult = delete(entity, options).awaitSingle()

/**
 * 엔티티를 삭제하고 [DeleteOptions]를 적용합니다.
 *
 * ## 동작/계약
 * - `delete(entity, options).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val result = reactiveOps.deleteSuspending(user, deleteOptions { timeout(300) })
 * // result.wasApplied() == true
 * ```
 */
suspend fun <T: Any> ReactiveCassandraOperations.deleteSuspending(
    entity: T,
    options: DeleteOptions,
): WriteResult = delete(entity, options).awaitSingle()

/**
 * id 기준으로 삭제합니다.
 *
 * ## 동작/계약
 * - `deleteById<T>(id).awaitSingle()`을 호출합니다.
 *
 * ```kotlin
 * val deleted = reactiveOps.deleteByIdSuspending<User>("user-1")
 * // deleted == true
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.deleteByIdSuspending(id: Any): Boolean =
    deleteById<T>(id).awaitSingle()

/**
 * 테이블을 truncate 합니다.
 *
 * ## 동작/계약
 * - `truncate<T>().awaitSingleOrNull()`을 호출합니다.
 * - 완료 시 반환값은 없습니다.
 *
 * ```kotlin
 * reactiveOps.truncateSuspending<User>()
 * ```
 */
suspend inline fun <reified T: Any> ReactiveCassandraOperations.truncateSuspending() {
    truncate<T>().awaitSingleOrNull()
}
