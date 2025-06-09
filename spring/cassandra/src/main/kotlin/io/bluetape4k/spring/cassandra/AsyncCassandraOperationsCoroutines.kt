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
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl

suspend fun AsyncCassandraOperations.suspendExecute(stmt: Statement<*>): AsyncResultSet =
    execute(stmt).await()

@Deprecated("Use suspendExecute instead", ReplaceWith("suspendExecute(stmt)"))
suspend fun AsyncCassandraOperations.coExecute(stmt: Statement<*>): AsyncResultSet =
    execute(stmt).await()


suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(statement: Statement<*>): List<T> =
    select(statement, T::class.java).await() ?: emptyList()

@Deprecated("Use suspendSelect instead", ReplaceWith("suspendSelect(statement)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(statement: Statement<*>): List<T> =
    select(statement, T::class.java).await() ?: emptyList()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(
    statement: Statement<*>,
    crossinline consumer: (T) -> Unit,
) {
    select(statement, { consumer(it) }, T::class.java).await()
}

@Deprecated("Use suspendSelect with consumer instead", ReplaceWith("suspendSelect(statement, consumer)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(
    statement: Statement<*>,
    crossinline consumer: (T) -> Unit,
) {
    select(statement, { consumer(it) }, T::class.java).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(cql: String): List<T> =
    suspendSelect(statementOf(cql))

@Deprecated("Use suspendSelect with cql instead", ReplaceWith("suspendSelect(cql)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(cql: String): List<T> =
    coSelect(statementOf(cql))

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(
    cql: String,
    crossinline consumer: (T) -> Unit,
) {
    suspendSelect(statementOf(cql), consumer)
}

@Deprecated("Use suspendSelect with cql and consumer instead", ReplaceWith("suspendSelect(cql, consumer)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(
    cql: String,
    crossinline consumer: (T) -> Unit,
) {
    coSelect(statementOf(cql), consumer)
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(
    query: Query,
    crossinline consumer: (T) -> Unit,
) {
    select(query, { consumer(it) }, T::class.java).await()
}

@Deprecated("Use suspendSelect with query and consumer instead", ReplaceWith("suspendSelect(query, consumer)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(
    query: Query,
    crossinline consumer: (T) -> Unit,
) {
    select(query, { consumer(it) }, T::class.java).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelectOneOrNull(statement: Statement<*>): T? {
    return selectOne(statement, T::class.java).await()
}

@Deprecated("Use suspendSelectOneOrNull instead", ReplaceWith("suspendSelectOneOrNull(statement)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneOrNull(statement: Statement<*>): T? {
    return selectOne(statement, T::class.java).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelectOneOrNull(cql: String): T? {
    return suspendSelectOneOrNull(statementOf(cql))
}

@Deprecated("Use suspendSelectOneOrNull with cql instead", ReplaceWith("suspendSelectOneOrNull(cql)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneOrNull(cql: String): T? {
    return coSelectOneOrNull(statementOf(cql))
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelect(query: Query): List<T> =
    select(query, T::class.java).await() ?: emptyList()

@Deprecated("Use suspendSelect with query instead", ReplaceWith("suspendSelect(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(query: Query): List<T> =
    select(query, T::class.java).await() ?: emptyList()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelectOneOrNull(query: Query): T? =
    selectOne(query, T::class.java).await()

@Deprecated("Use suspendSelectOneOrNull with query instead", ReplaceWith("suspendSelectOneOrNull(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneOrNull(query: Query): T? =
    selectOne(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSlice(statement: Statement<*>): Slice<T> =
    slice(statement, T::class.java).await() ?: SliceImpl(emptyList())

@Deprecated("Use suspendSlice instead", ReplaceWith("suspendSlice(statement)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSlice(statement: Statement<*>): Slice<T> =
    slice(statement, T::class.java).await() ?: SliceImpl(emptyList())

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSlice(query: Query): Slice<T> =
    slice(query, T::class.java).await() ?: SliceImpl(emptyList())

@Deprecated("Use suspendSlice with query instead", ReplaceWith("suspendSlice(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSlice(query: Query): Slice<T> =
    slice(query, T::class.java).await() ?: SliceImpl(emptyList())

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendUpdate(query: Query, update: Update): Boolean? =
    update(query, update, T::class.java).await()

@Deprecated("Use suspendUpdate with query and update instead", ReplaceWith("suspendUpdate(query, update)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coUpdate(query: Query, update: Update): Boolean? =
    update(query, update, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendDelete(query: Query): Boolean? =
    delete(query, T::class.java).await()

@Deprecated("Use suspendDelete with query instead", ReplaceWith("suspendDelete(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coDelete(query: Query): Boolean? =
    delete(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendCount(): Long? =
    count(T::class.java).await()

@Deprecated("Use suspendCount instead", ReplaceWith("suspendCount()"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coCount(): Long? =
    count(T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendCount(query: Query): Long? =
    count(query, T::class.java).await()

@Deprecated("Use suspendCount with query instead", ReplaceWith("suspendCount(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coCount(query: Query): Long? =
    count(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendExists(id: Any): Boolean? =
    exists(id, T::class.java).await()

@Deprecated("Use suspendExists with id instead", ReplaceWith("suspendExists(id)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coExists(id: Any): Boolean? =
    exists(id, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendExists(query: Query): Boolean? =
    exists(query, T::class.java).await()

@Deprecated("Use suspendExists with query instead", ReplaceWith("suspendExists(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coExists(query: Query): Boolean? =
    exists(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendSelectOneById(id: Any): T? =
    selectOneById(id, T::class.java).await()

@Deprecated("Use suspendSelectOneById instead", ReplaceWith("suspendSelectOneById(id)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneById(id: Any): T? =
    selectOneById(id, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendDeleteById(id: Any): Boolean? =
    deleteById(id, T::class.java).await()

@Deprecated("Use suspendDeleteById instead", ReplaceWith("suspendDeleteById(id)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coDeleteById(id: Any): Boolean? =
    deleteById(id, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.suspendTruncate() {
    truncate(T::class.java).await()
}

@Deprecated("Use suspendTruncate instead", ReplaceWith("suspendTruncate()"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coTruncate() {
    truncate(T::class.java).await()
}

suspend fun <T: Any> AsyncCassandraOperations.suspendInsert(entity: T): T? =
    insert(entity).await()

@Deprecated("Use suspendInsert instead", ReplaceWith("suspendInsert(entity)"))
suspend fun <T: Any> AsyncCassandraOperations.coInsert(entity: T): T? =
    insert(entity).await()

suspend fun <T: Any> AsyncCassandraOperations.suspendInsert(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> =
    insert(entity, options).await()

@Deprecated("Use suspendInsert with options instead", ReplaceWith("suspendInsert(entity, options)"))
suspend fun <T: Any> AsyncCassandraOperations.coInsert(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> =
    insert(entity, options).await()

suspend fun <T: Any> AsyncCassandraOperations.suspendUpdate(entity: T): T? =
    update(entity).await()

@Deprecated("Use suspendUpdate instead", ReplaceWith("suspendUpdate(entity)"))
suspend fun <T: Any> AsyncCassandraOperations.coUpdate(entity: T): T? =
    update(entity).await()

suspend fun <T: Any> AsyncCassandraOperations.suspendUpdate(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> =
    update(entity, options).await()

@Deprecated("Use suspendUpdate with options instead", ReplaceWith("suspendUpdate(entity, options)"))
suspend fun <T: Any> AsyncCassandraOperations.coUpdate(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> =
    update(entity, options).await()

suspend fun <T: Any> AsyncCassandraOperations.suspendDelete(entity: T): T? =
    delete(entity).await()

@Deprecated("Use suspendDelete instead", ReplaceWith("suspendDelete(entity)"))
suspend fun <T: Any> AsyncCassandraOperations.coDelete(entity: T): T? =
    delete(entity).await()

suspend fun AsyncCassandraOperations.suspendDelete(entity: Any, options: DeleteOptions): WriteResult =
    delete(entity, options).await()

@Deprecated("Use suspendDelete with options instead", ReplaceWith("suspendDelete(entity, options)"))
suspend fun AsyncCassandraOperations.coDelete(entity: Any, options: DeleteOptions): WriteResult =
    delete(entity, options).await()
