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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(statement: Statement<*>): Flow<T> =
    select<T>(statement).asFlow()

inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(cql: String): Flow<T> =
    select<T>(cql).asFlow()

inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(query: Query): Flow<T> =
    select<T>(query).asFlow()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOne(statement: Statement<*>): T =
    selectOne<T>(statement).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOneOrNull(statement: Statement<*>): T? =
    selectOne<T>(statement).awaitSingleOrNull()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOne(cql: String): T =
    selectOne<T>(cql).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOneOrNull(cql: String): T? =
    selectOne<T>(cql).awaitSingleOrNull()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOne(query: Query): T =
    selectOne<T>(query).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOneOrNull(query: Query): T? =
    selectOne<T>(query).awaitSingleOrNull()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSlice(statement: Statement<*>): Slice<T> =
    slice<T>(statement).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSlice(query: Query): Slice<T> =
    slice<T>(query).awaitSingle()

suspend fun ReactiveCassandraOperations.suspendExecute(statement: Statement<*>): ReactiveResultSet =
    execute(statement).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendUpdate(query: Query, update: Update): Boolean =
    update<T>(query, update).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendDelete(query: Query): Boolean =
    delete<T>(query).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendCount(): Long =
    count<T>().awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendCount(query: Query): Long =
    count<T>(query).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendExists(): Boolean =
    query<T>().exists().awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendExists(query: Query): Boolean =
    exists<T>(query).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendExists(id: Any): Boolean =
    exists<T>(id).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOneById(id: Any): T =
    selectOneById<T>(id).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendSelectOneOrNullById(id: Any): T? =
    selectOneById<T>(id).awaitSingleOrNull()

suspend fun <T: Any> ReactiveCassandraOperations.suspendInsert(entity: T): T =
    insert(entity).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.suspendInsert(
    entity: T,
    options: InsertOptions,
): EntityWriteResult<T> =
    insert(entity, options).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.suspendUpdate(entity: T): T =
    update(entity).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.suspendUpdate(
    entity: T,
    options: UpdateOptions,
): EntityWriteResult<T> =
    update(entity, options).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.suspendDelete(entity: T): T =
    delete(entity).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.suspendDelete(entity: T, options: QueryOptions): WriteResult =
    delete(entity, options).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.suspendDelete(entity: T, options: DeleteOptions): WriteResult =
    delete(entity, options).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendDeleteById(id: Any): Boolean =
    deleteById<T>(id).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.suspendTruncate() {
    truncate<T>().awaitSingleOrNull()
}


inline fun <reified T: Any> ReactiveCassandraOperations.count(): Mono<Long> =
    count<T>()

inline fun <reified T: Any> ReactiveCassandraOperations.select(statement: Statement<*>): Flux<T> =
    select<T>(statement)

inline fun <reified T: Any> ReactiveCassandraOperations.select(cql: String): Flux<T> =
    select<T>(cql)

inline fun <reified T: Any> ReactiveCassandraOperations.select(query: Query): Flux<T> =
    select<T>(query)

inline fun <reified T: Any> ReactiveCassandraOperations.selectOne(statement: Statement<*>): Mono<T> =
    selectOne<T>(statement)

inline fun <reified T: Any> ReactiveCassandraOperations.selectOne(cql: String): Mono<T> =
    selectOne<T>(cql)

inline fun <reified T: Any> ReactiveCassandraOperations.selectOne(query: Query): Mono<T> =
    selectOne<T>(query)

inline fun <reified T: Any> ReactiveCassandraOperations.slice(statement: Statement<*>): Mono<Slice<T>> =
    slice(statement, T::class.java)

inline fun <reified T: Any> ReactiveCassandraOperations.slice(query: Query): Mono<Slice<T>> =
    slice(query, T::class.java)
