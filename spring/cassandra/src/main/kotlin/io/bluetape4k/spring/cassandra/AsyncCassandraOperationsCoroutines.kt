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

suspend fun AsyncCassandraOperations.awaitExecute(stmt: Statement<*>): AsyncResultSet =
    execute(stmt).await()

@Deprecated("Use awaitExecute instead", ReplaceWith("awaitExecute(stmt)"))
suspend fun AsyncCassandraOperations.coExecute(stmt: Statement<*>): AsyncResultSet =
    execute(stmt).await()


suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSelect(statement: Statement<*>): List<T> =
    select(statement, T::class.java).await() ?: emptyList()

@Deprecated("Use awaitSelect instead", ReplaceWith("awaitSelect(statement)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(statement: Statement<*>): List<T> =
    select(statement, T::class.java).await() ?: emptyList()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSelect(
    statement: Statement<*>,
    crossinline consumer: (T) -> Unit,
) {
    select(statement, { consumer(it) }, T::class.java).await()
}

@Deprecated("Use awaitSelect with consumer instead", ReplaceWith("awaitSelect(statement, consumer)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(
    statement: Statement<*>,
    crossinline consumer: (T) -> Unit,
) {
    select(statement, { consumer(it) }, T::class.java).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSelect(cql: String): List<T> =
    awaitSelect(statementOf(cql))

@Deprecated("Use awaitSelect with cql instead", ReplaceWith("awaitSelect(cql)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(cql: String): List<T> =
    coSelect(statementOf(cql))

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSelect(
    cql: String,
    crossinline consumer: (T) -> Unit,
) {
    awaitSelect(statementOf(cql), consumer)
}

@Deprecated("Use awaitSelect with cql and consumer instead", ReplaceWith("awaitSelect(cql, consumer)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(
    cql: String,
    crossinline consumer: (T) -> Unit,
) {
    coSelect(statementOf(cql), consumer)
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSelect(
    query: Query,
    crossinline consumer: (T) -> Unit,
) {
    select(query, { consumer(it) }, T::class.java).await()
}

@Deprecated("Use awaitSelect with query and consumer instead", ReplaceWith("awaitSelect(query, consumer)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(
    query: Query,
    crossinline consumer: (T) -> Unit,
) {
    select(query, { consumer(it) }, T::class.java).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSelectOneOrNull(statement: Statement<*>): T? {
    return selectOne(statement, T::class.java).await()
}

@Deprecated("Use awaitSelectOneOrNull instead", ReplaceWith("awaitSelectOneOrNull(statement)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneOrNull(statement: Statement<*>): T? {
    return selectOne(statement, T::class.java).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSelectOneOrNull(cql: String): T? {
    return awaitSelectOneOrNull(statementOf(cql))
}

@Deprecated("Use awaitSelectOneOrNull with cql instead", ReplaceWith("awaitSelectOneOrNull(cql)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneOrNull(cql: String): T? {
    return coSelectOneOrNull(statementOf(cql))
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSelect(query: Query): List<T> =
    select(query, T::class.java).await() ?: emptyList()

@Deprecated("Use awaitSelect with query instead", ReplaceWith("awaitSelect(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(query: Query): List<T> =
    select(query, T::class.java).await() ?: emptyList()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSelectOneOrNull(query: Query): T? =
    selectOne(query, T::class.java).await()

@Deprecated("Use awaitSelectOneOrNull with query instead", ReplaceWith("awaitSelectOneOrNull(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneOrNull(query: Query): T? =
    selectOne(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSlice(statement: Statement<*>): Slice<T> =
    slice(statement, T::class.java).await() ?: SliceImpl(emptyList())

@Deprecated("Use awaitSlice instead", ReplaceWith("awaitSlice(statement)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSlice(statement: Statement<*>): Slice<T> =
    slice(statement, T::class.java).await() ?: SliceImpl(emptyList())

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSlice(query: Query): Slice<T> =
    slice(query, T::class.java).await() ?: SliceImpl(emptyList())

@Deprecated("Use awaitSlice with query instead", ReplaceWith("awaitSlice(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSlice(query: Query): Slice<T> =
    slice(query, T::class.java).await() ?: SliceImpl(emptyList())

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitUpdate(query: Query, update: Update): Boolean? =
    update(query, update, T::class.java).await()

@Deprecated("Use awaitUpdate with query and update instead", ReplaceWith("awaitUpdate(query, update)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coUpdate(query: Query, update: Update): Boolean? =
    update(query, update, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitDelete(query: Query): Boolean? =
    delete(query, T::class.java).await()

@Deprecated("Use awaitDelete with query instead", ReplaceWith("awaitDelete(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coDelete(query: Query): Boolean? =
    delete(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitCount(): Long? =
    count(T::class.java).await()

@Deprecated("Use awaitCount instead", ReplaceWith("awaitCount()"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coCount(): Long? =
    count(T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitCount(query: Query): Long? =
    count(query, T::class.java).await()

@Deprecated("Use awaitCount with query instead", ReplaceWith("awaitCount(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coCount(query: Query): Long? =
    count(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitExists(id: Any): Boolean? =
    exists(id, T::class.java).await()

@Deprecated("Use awaitExists with id instead", ReplaceWith("awaitExists(id)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coExists(id: Any): Boolean? =
    exists(id, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitExists(query: Query): Boolean? =
    exists(query, T::class.java).await()

@Deprecated("Use awaitExists with query instead", ReplaceWith("awaitExists(query)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coExists(query: Query): Boolean? =
    exists(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitSelectOneById(id: Any): T? =
    selectOneById(id, T::class.java).await()

@Deprecated("Use awaitSelectOneById instead", ReplaceWith("awaitSelectOneById(id)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneById(id: Any): T? =
    selectOneById(id, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitDeleteById(id: Any): Boolean? =
    deleteById(id, T::class.java).await()

@Deprecated("Use awaitDeleteById instead", ReplaceWith("awaitDeleteById(id)"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coDeleteById(id: Any): Boolean? =
    deleteById(id, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.awaitTruncate() {
    truncate(T::class.java).await()
}

@Deprecated("Use awaitTruncate instead", ReplaceWith("awaitTruncate()"))
suspend inline fun <reified T: Any> AsyncCassandraOperations.coTruncate() {
    truncate(T::class.java).await()
}

suspend fun <T: Any> AsyncCassandraOperations.awaitInsert(entity: T): T? =
    insert(entity).await()

@Deprecated("Use awaitInsert instead", ReplaceWith("awaitInsert(entity)"))
suspend fun <T: Any> AsyncCassandraOperations.coInsert(entity: T): T? =
    insert(entity).await()

suspend fun <T: Any> AsyncCassandraOperations.awaitInsert(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> =
    insert(entity, options).await()

@Deprecated("Use awaitInsert with options instead", ReplaceWith("awaitInsert(entity, options)"))
suspend fun <T: Any> AsyncCassandraOperations.coInsert(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> =
    insert(entity, options).await()

suspend fun <T: Any> AsyncCassandraOperations.awaitUpdate(entity: T): T? =
    update(entity).await()

@Deprecated("Use awaitUpdate instead", ReplaceWith("awaitUpdate(entity)"))
suspend fun <T: Any> AsyncCassandraOperations.coUpdate(entity: T): T? =
    update(entity).await()

suspend fun <T: Any> AsyncCassandraOperations.awaitUpdate(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> =
    update(entity, options).await()

@Deprecated("Use awaitUpdate with options instead", ReplaceWith("awaitUpdate(entity, options)"))
suspend fun <T: Any> AsyncCassandraOperations.coUpdate(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> =
    update(entity, options).await()

suspend fun <T: Any> AsyncCassandraOperations.awaitDelete(entity: T): T? =
    delete(entity).await()

@Deprecated("Use awaitDelete instead", ReplaceWith("awaitDelete(entity)"))
suspend fun <T: Any> AsyncCassandraOperations.coDelete(entity: T): T? =
    delete(entity).await()

suspend fun AsyncCassandraOperations.awaitDelete(entity: Any, options: DeleteOptions): WriteResult =
    delete(entity, options).await()

@Deprecated("Use awaitDelete with options instead", ReplaceWith("awaitDelete(entity, options)"))
suspend fun AsyncCassandraOperations.coDelete(entity: Any, options: DeleteOptions): WriteResult =
    delete(entity, options).await()
