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

suspend fun AsyncCassandraOperations.suspendExecute(stmt: Statement<*>): AsyncResultSet =
    execute(stmt).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(statement: Statement<*>): List<T> =
    select<T>(statement).await() ?: emptyList()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(
    statement: Statement<*>,
    crossinline consumer: (T) -> Unit,
) {
    select<T>(statement) { consumer(it) }.await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(cql: String): List<T> =
    suspendSelect(statementOf(cql))

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(
    cql: String,
    crossinline consumer: (T) -> Unit,
) {
    suspendSelect(statementOf(cql), consumer)
}

// Spring 원본의 실수로 select 함수를 deprecate 시켰음
@Suppress("DEPRECATION")
suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(
    query: Query,
    crossinline consumer: (T) -> Unit,
) {
    select<T>(query, consumer).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelectOneOrNull(statement: Statement<*>): T? {
    return selectOne<T>(statement).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelectOneOrNull(cql: String): T? {
    return suspendSelectOneOrNull(statementOf(cql))
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(query: Query): List<T> =
    select<T>(query).await() ?: emptyList()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelectOneOrNull(query: Query): T? =
    selectOne<T>(query).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSlice(statement: Statement<*>): Slice<T> =
    slice<T>(statement).await() ?: SliceImpl(emptyList())

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSlice(query: Query): Slice<T> =
    slice<T>(query).await() ?: SliceImpl(emptyList())

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendUpdate(query: Query, update: Update): Boolean? =
    update<T>(query, update).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendDelete(query: Query): Boolean? =
    delete<T>(query).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendCount(): Long? =
    count<T>().await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendCount(query: Query): Long? =
    count<T>(query).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendExists(id: Any): Boolean? =
    exists<T>(id).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendExists(query: Query): Boolean? =
    exists<T>(query).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelectOneById(id: Any): T? =
    selectOneById<T>(id).await()

suspend fun <T: Any> AsyncCassandraOperations.suspendInsert(entity: T): T? =
    insert(entity).await()

suspend fun <T: Any> AsyncCassandraOperations.suspendInsert(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> =
    insert(entity, options).await()

suspend fun <T: Any> AsyncCassandraOperations.suspendUpdate(entity: T): T? =
    update(entity).await()

suspend fun <T: Any> AsyncCassandraOperations.suspendUpdate(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> =
    update(entity, options).await()

suspend fun <T: Any> AsyncCassandraOperations.suspendDelete(entity: T): T? =
    delete(entity).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendDeleteById(id: Any): Boolean =
    deleteById<T>(id).await()

suspend fun AsyncCassandraOperations.suspendDelete(entity: Any, options: DeleteOptions): WriteResult =
    delete(entity, options).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendTruncate() {
    truncate<T>().await()
}
