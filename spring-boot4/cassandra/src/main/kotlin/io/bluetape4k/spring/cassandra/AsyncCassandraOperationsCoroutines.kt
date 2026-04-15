package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.datastax.oss.driver.api.core.cql.Statement
import io.bluetape4k.cassandra.cql.statementOf
import kotlinx.coroutines.future.await
import org.springframework.data.cassandra.core.AsyncCassandraOperations
import org.springframework.data.cassandra.core.DeleteOptions
import org.springframework.data.cassandra.core.EntityWriteResult
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.data.cassandra.core.WriteResult
import org.springframework.data.cassandra.core.count
import org.springframework.data.cassandra.core.delete
import org.springframework.data.cassandra.core.deleteById
import org.springframework.data.cassandra.core.exists
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.cassandra.core.select
import org.springframework.data.cassandra.core.selectOne
import org.springframework.data.cassandra.core.selectOneById
import org.springframework.data.cassandra.core.slice
import org.springframework.data.cassandra.core.truncate
import org.springframework.data.cassandra.core.update
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl

/**
 * [Statement]를 실행하고 [AsyncResultSet]을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `execute(stmt).await()`를 호출합니다.
 * - CQL 실행 실패 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val rs = asyncOps.executeSuspending(boundStatement)
 * // rs.remainingInPage() >= 0
 * ```
 */
suspend fun AsyncCassandraOperations.executeSuspending(stmt: Statement<*>): AsyncResultSet =
    execute(stmt).await()

/**
 * [Statement]로 조회하고 결과를 리스트로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `select<T>(statement).await()`를 호출합니다.
 * - 결과가 없거나 `null`이면 빈 리스트를 반환합니다.
 *
 * ```kotlin
 * val users = asyncOps.selectSuspending<User>(statement)
 * // users.size >= 0
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.selectSuspending(statement: Statement<*>): List<T> =
    select<T>(statement).await() ?: emptyList()

/**
 * [Statement]로 조회하고 각 원소에 대해 [consumer]를 수행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `select<T>(statement) { consumer(it) }.await()`를 호출합니다.
 * - 결과가 없으면 [consumer]는 호출되지 않습니다.
 *
 * ```kotlin
 * asyncOps.selectSuspending<User>(statement) { user ->
 *     println(user.name)
 * }
 * // 각 user.name이 출력됨
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.selectSuspending(
    statement: Statement<*>,
    crossinline consumer: (T) -> Unit,
) {
    select<T>(statement) { consumer(it) }.await()
}

/**
 * CQL 문자열로 조회하고 결과를 리스트로 반환합니다.
 *
 * ## 동작/계약
 * - `statementOf(cql)`로 Statement를 생성해 [selectSuspending] 오버로드에 위임합니다.
 * - 결과가 없으면 빈 리스트를 반환합니다.
 *
 * ```kotlin
 * val users = asyncOps.selectSuspending<User>("SELECT * FROM users")
 * // users.size >= 0
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.selectSuspending(cql: String): List<T> =
    selectSuspending(statementOf(cql))

/**
 * CQL 문자열로 조회하고 각 원소에 대해 [consumer]를 수행합니다.
 *
 * ## 동작/계약
 * - `statementOf(cql)`로 Statement를 생성해 위임합니다.
 * - 결과가 없으면 [consumer]는 호출되지 않습니다.
 *
 * ```kotlin
 * asyncOps.selectSuspending<User>("SELECT * FROM users") { user ->
 *     println(user.name)
 * }
 * // 각 user.name이 출력됨
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.selectSuspending(
    cql: String,
    crossinline consumer: (T) -> Unit,
) {
    selectSuspending(statementOf(cql), consumer)
}

// Spring 원본의 실수로 select 함수를 deprecate 시켰음
@Suppress("DEPRECATION")
/**
 * [Query]로 조회하고 각 원소에 대해 [consumer]를 수행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `select<T>(query, consumer).await()`를 호출합니다.
 * - 결과가 없으면 [consumer]는 호출되지 않습니다.
 *
 * ```kotlin
 * asyncOps.selectSuspending<User>(Query.empty()) { user ->
 *     println(user.name)
 * }
 * // 각 user.name이 출력됨
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.selectSuspending(
    query: Query,
    crossinline consumer: (T) -> Unit,
) {
    select(query, T::class.java).await().forEach { consumer(it) }
}

/**
 * [Statement]로 단건을 조회하고 없으면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `selectOne<T>(statement).await()`를 호출합니다.
 * - 결과가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val user = asyncOps.selectOneOrNullSuspending<User>(statement)
 * // user == null || user is User
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.selectOneOrNullSuspending(statement: Statement<*>): T? =
    selectOne<T>(statement).await()

/**
 * CQL 문자열로 단건을 조회하고 없으면 null을 반환합니다.
 *
 * ## 동작/계약
 * - `statementOf(cql)`로 Statement를 생성해 [selectOneOrNullSuspending] 오버로드에 위임합니다.
 *
 * ```kotlin
 * val user = asyncOps.selectOneOrNullSuspending<User>("SELECT * FROM users WHERE id = 'user-1'")
 * // user == null || user is User
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.selectOneOrNullSuspending(cql: String): T? =
    selectOneOrNullSuspending(statementOf(cql))

/**
 * [Query]로 조회하고 결과를 리스트로 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `select<T>(query).await()`를 호출합니다.
 * - 결과가 없거나 `null`이면 빈 리스트를 반환합니다.
 *
 * ```kotlin
 * val users = asyncOps.selectSuspending<User>(Query.empty())
 * // users.size >= 0
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.selectSuspending(query: Query): List<T> =
    select<T>(query).await() ?: emptyList()

/**
 * [Query]로 단건을 조회하고 없으면 null을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `selectOne<T>(query).await()`를 호출합니다.
 * - 결과가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val user = asyncOps.selectOneOrNullSuspending<User>(Query.empty())
 * // user == null || user is User
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.selectOneOrNullSuspending(query: Query): T? =
    selectOne<T>(query).await()

/**
 * [Statement]로 [Slice]를 조회합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `slice<T>(statement).await()`를 호출합니다.
 * - 결과가 없거나 `null`이면 빈 SliceImpl을 반환합니다.
 *
 * ```kotlin
 * val slice = asyncOps.sliceSuspending<User>(statement)
 * // slice.content.size >= 0
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.sliceSuspending(statement: Statement<*>): Slice<T> =
    slice<T>(statement).await() ?: SliceImpl(emptyList())

/**
 * [Query]로 [Slice]를 조회합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `slice<T>(query).await()`를 호출합니다.
 * - 결과가 없거나 `null`이면 빈 SliceImpl을 반환합니다.
 *
 * ```kotlin
 * val slice = asyncOps.sliceSuspending<User>(Query.empty())
 * // slice.content.size >= 0
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.sliceSuspending(query: Query): Slice<T> =
    slice<T>(query).await() ?: SliceImpl(emptyList())

/**
 * [Query]와 [Update]로 갱신하고 성공 여부를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `update<T>(query, update).await()`를 호출합니다.
 * - 갱신 성공 여부 또는 `null`을 반환합니다.
 *
 * ```kotlin
 * val updated = asyncOps.updateSuspending<User>(query, update)
 * // updated == true || updated == null
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.updateSuspending(
    query: Query,
    update: Update,
): Boolean? = update<T>(query, update).await()

/**
 * [Query]로 삭제하고 성공 여부를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `delete<T>(query).await()`를 호출합니다.
 * - 삭제 성공 여부 또는 `null`을 반환합니다.
 *
 * ```kotlin
 * val deleted = asyncOps.deleteSuspending<User>(Query.empty())
 * // deleted == true || deleted == null
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.deleteSuspending(query: Query): Boolean? =
    delete<T>(query).await()

/**
 * 전체 건수를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `count<T>().await()`를 호출합니다.
 * - 결과 또는 `null`을 반환합니다.
 *
 * ```kotlin
 * val count = asyncOps.countSuspending<User>()
 * // count != null && count >= 0
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.countSuspending(): Long? = count<T>().await()

/**
 * [Query] 조건의 건수를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `count<T>(query).await()`를 호출합니다.
 * - 결과 또는 `null`을 반환합니다.
 *
 * ```kotlin
 * val count = asyncOps.countSuspending<User>(Query.empty())
 * // count != null && count >= 0
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.countSuspending(query: Query): Long? =
    count<T>(query).await()

/**
 * id 기준으로 존재 여부를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `exists<T>(id).await()`를 호출합니다.
 * - 결과 또는 `null`을 반환합니다.
 *
 * ```kotlin
 * val exists = asyncOps.existsSuspending<User>("user-1")
 * // exists == true || exists == null
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.existsSuspending(id: Any): Boolean? =
    exists<T>(id).await()

/**
 * [Query] 조건으로 존재 여부를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `exists<T>(query).await()`를 호출합니다.
 * - 결과 또는 `null`을 반환합니다.
 *
 * ```kotlin
 * val exists = asyncOps.existsSuspending<User>(Query.empty())
 * // exists == true || exists == null
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.existsSuspending(query: Query): Boolean? =
    exists<T>(query).await()

/**
 * id 기준으로 단건을 조회합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `selectOneById<T>(id).await()`를 호출합니다.
 * - 결과가 없으면 `null`을 반환합니다.
 *
 * ```kotlin
 * val user = asyncOps.selectOneByIdSuspending<User>("user-1")
 * // user == null || user is User
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.selectOneByIdSuspending(id: Any): T? =
    selectOneById<T>(id).await()

/**
 * 엔티티를 저장하고 저장된 엔티티를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `insert(entity).await()`를 호출합니다.
 * - 저장 성공 시 저장된 엔티티를, 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val saved = asyncOps.insertSuspending(user)
 * // saved == null || saved is User
 * ```
 */
suspend fun <T: Any> AsyncCassandraOperations.insertSuspending(entity: T): T? = insert(entity).await()

/**
 * 엔티티를 저장하고 [InsertOptions]를 적용합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `insert(entity, options).await()`를 호출합니다.
 * - LWT/TTL 등 옵션이 반영된 [EntityWriteResult]를 반환합니다.
 *
 * ```kotlin
 * val result = asyncOps.insertSuspending(user, insertOptions { withIfNotExists() })
 * // result.wasApplied() == true
 * ```
 */
suspend fun <T: Any> AsyncCassandraOperations.insertSuspending(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> =
    insert(entity, options).await()

/**
 * 엔티티를 갱신하고 갱신된 엔티티를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `update(entity).await()`를 호출합니다.
 * - 갱신 성공 시 엔티티를, 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val updated = asyncOps.updateSuspending(user)
 * // updated == null || updated is User
 * ```
 */
suspend fun <T: Any> AsyncCassandraOperations.updateSuspending(entity: T): T? = update(entity).await()

/**
 * 엔티티를 갱신하고 [UpdateOptions]를 적용합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `update(entity, options).await()`를 호출합니다.
 * - LWT 등 옵션이 반영된 [EntityWriteResult]를 반환합니다.
 *
 * ```kotlin
 * val result = asyncOps.updateSuspending(user, updateOptions { timeout(300) })
 * // result.wasApplied() == true
 * ```
 */
suspend fun <T: Any> AsyncCassandraOperations.updateSuspending(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> =
    update(entity, options).await()

/**
 * 엔티티를 삭제하고 삭제된 엔티티를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `delete(entity).await()`를 호출합니다.
 * - 삭제 성공 시 엔티티를, 실패 시 `null`을 반환합니다.
 *
 * ```kotlin
 * val deleted = asyncOps.deleteSuspending(user)
 * // deleted == null || deleted is User
 * ```
 */
suspend fun <T: Any> AsyncCassandraOperations.deleteSuspending(entity: T): T? = delete(entity).await()

/**
 * id 기준으로 삭제합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `deleteById<T>(id).await()`를 호출합니다.
 * - 삭제 성공 여부를 반환합니다.
 *
 * ```kotlin
 * val deleted = asyncOps.deleteByIdSuspending<User>("user-1")
 * // deleted == true
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.deleteByIdSuspending(id: Any): Boolean =
    deleteById<T>(id).await()

/**
 * 엔티티를 삭제하고 [DeleteOptions]를 적용합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `delete(entity, options).await()`를 호출합니다.
 * - 타임스탬프 등 옵션이 반영된 [WriteResult]를 반환합니다.
 *
 * ```kotlin
 * val result = asyncOps.deleteSuspending(user, deleteOptions { timeout(300) })
 * // result.wasApplied() == true
 * ```
 */
suspend fun AsyncCassandraOperations.deleteSuspending(
    entity: Any,
    options: DeleteOptions,
): WriteResult =
    delete(entity, options).await()

/**
 * 테이블을 truncate 합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `truncate<T>().await()`를 호출합니다.
 * - 완료 시 반환값은 없으며 테이블의 모든 데이터가 삭제됩니다.
 *
 * ```kotlin
 * asyncOps.truncateSuspending<User>()
 * // User 테이블의 모든 데이터가 삭제됨
 * ```
 */
suspend inline fun <reified T: Any> AsyncCassandraOperations.truncateSuspending() {
    truncate<T>().await()
}
