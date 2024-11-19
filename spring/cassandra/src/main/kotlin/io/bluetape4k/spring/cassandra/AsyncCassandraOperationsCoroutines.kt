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

suspend fun AsyncCassandraOperations.coExecute(stmt: Statement<*>): AsyncResultSet =
    execute(stmt).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(statement: Statement<*>): List<T> =
    select(statement, T::class.java).await() ?: emptyList()

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(
    statement: Statement<*>,
    crossinline consumer: (T) -> Unit,
) {
    select(statement, { consumer(it) }, T::class.java).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(cql: String): List<T> =
    coSelect(statementOf(cql))

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(
    cql: String,
    crossinline consumer: (T) -> Unit,
) {
    coSelect(statementOf(cql), consumer)
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(
    query: Query,
    crossinline consumer: (T) -> Unit,
) {
    select(query, { consumer(it) }, T::class.java).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneOrNull(statement: Statement<*>): T? {
    return selectOne(statement, T::class.java).await()
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneOrNull(cql: String): T? {
    return coSelectOneOrNull(statementOf(cql))
}

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelect(query: Query): List<T> =
    select(query, T::class.java).await() ?: emptyList()

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectOneOrNull(query: Query): T? =
    selectOne(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSlice(statement: Statement<*>): Slice<T> =
    slice(statement, T::class.java).await() ?: SliceImpl(emptyList())

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSlice(query: Query): Slice<T> =
    slice(query, T::class.java).await() ?: SliceImpl(emptyList())

suspend inline fun <reified T: Any> AsyncCassandraOperations.coUpdate(query: Query, update: Update): Boolean? =
    update(query, update, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.coDelete(query: Query): Boolean? =
    delete(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.coCount(): Long? =
    count(T::class.java).await()


suspend inline fun <reified T: Any> AsyncCassandraOperations.coCount(query: Query): Long? =
    count(query, T::class.java).await()


suspend inline fun <reified T: Any> AsyncCassandraOperations.coExists(id: Any): Boolean? =
    exists(id, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.coExists(query: Query): Boolean? =
    exists(query, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.coSelectById(id: Any): T? =
    selectOneById(id, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.coDeleteById(id: Any): Boolean? =
    deleteById(id, T::class.java).await()

suspend inline fun <reified T: Any> AsyncCassandraOperations.coTruncate() {
    truncate(T::class.java).await()
}

suspend fun <T: Any> AsyncCassandraOperations.coInsert(entity: T): T? =
    insert(entity).await()

suspend fun <T: Any> AsyncCassandraOperations.coInsert(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> =
    insert(entity, options).await()

suspend fun <T: Any> AsyncCassandraOperations.coUpdate(entity: T): T? =
    update(entity).await()

suspend fun <T: Any> AsyncCassandraOperations.coUpdate(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> =
    update(entity, options).await()

suspend fun <T: Any> AsyncCassandraOperations.coDelete(entity: T): T? =
    delete(entity).await()

suspend fun AsyncCassandraOperations.coDelete(entity: Any, options: DeleteOptions): WriteResult =
    delete(entity, options).await()
