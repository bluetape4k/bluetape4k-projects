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
 */
suspend fun AsyncCassandraOperations.executeSuspending(stmt: Statement<*>): AsyncResultSet = execute(stmt).await()

/**
 * [Statement]로 조회하고 결과를 리스트로 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.selectSuspending(statement: Statement<*>): List<T> =
    select<T>(statement).await() ?: emptyList()

/**
 * [Statement]로 조회하고 각 원소에 대해 [consumer]를 수행합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.selectSuspending(
    statement: Statement<*>,
    crossinline consumer: (T) -> Unit,
) {
    select<T>(statement) { consumer(it) }.await()
}

/**
 * CQL 문자열로 조회하고 결과를 리스트로 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.selectSuspending(cql: String): List<T> =
    selectSuspending(statementOf(cql))

/**
 * CQL 문자열로 조회하고 각 원소에 대해 [consumer]를 수행합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.selectSuspending(
    cql: String,
    crossinline consumer: (T) -> Unit,
) {
    selectSuspending(statementOf(cql), consumer)
}

// Spring 원본의 실수로 select 함수를 deprecate 시켰음
@Suppress("DEPRECATION")
/**
 * [Query]로 조회하고 각 원소에 대해 [consumer]를 수행합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.selectSuspending(
    query: Query,
    crossinline consumer: (T) -> Unit,
) {
    select<T>(query, consumer).await()
}

/**
 * [Statement]로 단건을 조회하고 없으면 null을 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.selectOneOrNullSuspending(statement: Statement<*>): T? =
    selectOne<T>(statement).await()

/**
 * CQL 문자열로 단건을 조회하고 없으면 null을 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.selectOneOrNullSuspending(cql: String): T? =
    selectOneOrNullSuspending(statementOf(cql))

/**
 * [Query]로 조회하고 결과를 리스트로 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.selectSuspending(query: Query): List<T> =
    select<T>(query).await() ?: emptyList()

/**
 * [Query]로 단건을 조회하고 없으면 null을 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.selectOneOrNullSuspending(query: Query): T? =
    selectOne<T>(query).await()

/**
 * [Statement]로 [Slice]를 조회합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.sliceSuspending(statement: Statement<*>): Slice<T> =
    slice<T>(statement).await() ?: SliceImpl(emptyList())

/**
 * [Query]로 [Slice]를 조회합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.sliceSuspending(query: Query): Slice<T> =
    slice<T>(query).await() ?: SliceImpl(emptyList())

/**
 * [Query]와 [Update]로 갱신하고 성공 여부를 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.updateSuspending(
    query: Query,
    update: Update,
): Boolean? = update<T>(query, update).await()

/**
 * [Query]로 삭제하고 성공 여부를 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.deleteSuspending(query: Query): Boolean? =
    delete<T>(query).await()

/**
 * 전체 건수를 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.countSuspending(): Long? = count<T>().await()

/**
 * [Query] 조건의 건수를 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.countSuspending(query: Query): Long? =
    count<T>(query).await()

/**
 * id 기준으로 존재 여부를 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.existsSuspending(id: Any): Boolean? =
    exists<T>(id).await()

/**
 * [Query] 조건으로 존재 여부를 반환합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.existsSuspending(query: Query): Boolean? =
    exists<T>(query).await()

/**
 * id 기준으로 단건을 조회합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.selectOneByIdSuspending(id: Any): T? =
    selectOneById<T>(id).await()

/**
 * 엔티티를 저장하고 저장된 엔티티를 반환합니다.
 */
suspend fun <T : Any> AsyncCassandraOperations.insertSuspending(entity: T): T? = insert(entity).await()

/**
 * 엔티티를 저장하고 [InsertOptions]를 적용합니다.
 */
suspend fun <T : Any> AsyncCassandraOperations.insertSuspending(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> = insert(entity, options).await()

/**
 * 엔티티를 갱신하고 갱신된 엔티티를 반환합니다.
 */
suspend fun <T : Any> AsyncCassandraOperations.updateSuspending(entity: T): T? = update(entity).await()

/**
 * 엔티티를 갱신하고 [UpdateOptions]를 적용합니다.
 */
suspend fun <T : Any> AsyncCassandraOperations.updateSuspending(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> = update(entity, options).await()

/**
 * 엔티티를 삭제하고 삭제된 엔티티를 반환합니다.
 */
suspend fun <T : Any> AsyncCassandraOperations.deleteSuspending(entity: T): T? = delete(entity).await()

/**
 * id 기준으로 삭제합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.deleteByIdSuspending(id: Any): Boolean =
    deleteById<T>(id).await()

/**
 * 엔티티를 삭제하고 [DeleteOptions]를 적용합니다.
 */
suspend fun AsyncCassandraOperations.deleteSuspending(
    entity: Any,
    options: DeleteOptions,
): WriteResult = delete(entity, options).await()

/**
 * 테이블을 truncate 합니다.
 */
suspend inline fun <reified T : Any> AsyncCassandraOperations.truncateSuspending() {
    truncate<T>().await()
}
