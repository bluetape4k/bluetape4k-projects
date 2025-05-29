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
import org.springframework.data.cassandra.core.cql.QueryOptions
import org.springframework.data.cassandra.core.query
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.domain.Slice
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(statement: Statement<*>): Flow<T> =
    select(statement, T::class.java).asFlow()

inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(cql: String): Flow<T> =
    select(cql, T::class.java).asFlow()

inline fun <reified T: Any> ReactiveCassandraOperations.selectAsFlow(query: Query): Flow<T> =
    select(query, T::class.java).asFlow()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitSelectOne(statement: Statement<*>): T =
    selectOne(statement, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitSelectOneOrNull(statement: Statement<*>): T? =
    selectOne(statement, T::class.java).awaitSingleOrNull()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitSelectOne(cql: String): T =
    selectOne(cql, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitSelectOneOrNull(cql: String): T? =
    selectOne(cql, T::class.java).awaitSingleOrNull()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitSelectOne(query: Query): T =
    selectOne(query, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitSelectOneOrNull(query: Query): T? =
    selectOne(query, T::class.java).awaitSingleOrNull()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitSlice(statement: Statement<*>): Slice<T> =
    slice(statement, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitSlice(query: Query): Slice<T> =
    slice(query, T::class.java).awaitSingle()

suspend fun ReactiveCassandraOperations.awaitExecute(statement: Statement<*>): ReactiveResultSet =
    execute(statement).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitUpdate(query: Query, update: Update): Boolean =
    update(query, update, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitDelete(query: Query): Boolean =
    delete(query, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitCount(): Long =
    count(T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitCount(query: Query): Long =
    count(query, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitExists(): Boolean =
    query<T>().exists().awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitExists(query: Query): Boolean =
    exists(query, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitExists(id: Any): Boolean =
    exists(id, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitSelectOneById(id: Any): T =
    selectOneById(id, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitSelectOneOrNullById(id: Any): T? =
    selectOneById(id, T::class.java).awaitSingleOrNull()

suspend fun <T: Any> ReactiveCassandraOperations.awaitInsert(entity: T): T =
    insert(entity).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.awaitInsert(entity: T, options: InsertOptions): EntityWriteResult<T> =
    insert(entity, options).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.awaitUpdate(entity: T): T =
    update(entity).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.awaitUpdate(entity: T, options: UpdateOptions): EntityWriteResult<T> =
    update(entity, options).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.awaitDelete(entity: T): T =
    delete(entity).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.awaitDelete(entity: T, options: QueryOptions): WriteResult =
    delete(entity, options).awaitSingle()

suspend fun <T: Any> ReactiveCassandraOperations.awaitDelete(entity: T, options: DeleteOptions): WriteResult =
    delete(entity, options).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitDeleteById(id: Any): Boolean =
    deleteById(id, T::class.java).awaitSingle()

suspend inline fun <reified T: Any> ReactiveCassandraOperations.awaitTruncate() {
    truncate(T::class.java).awaitSingleOrNull()
}


inline fun <reified T: Any> ReactiveCassandraOperations.count(): Mono<Long> =
    count(T::class.java)

inline fun <reified T: Any> ReactiveCassandraOperations.select(statement: Statement<*>): Flux<T> =
    select(statement, T::class.java)

inline fun <reified T: Any> ReactiveCassandraOperations.select(cql: String): Flux<T> =
    select(cql, T::class.java)

inline fun <reified T: Any> ReactiveCassandraOperations.select(query: Query): Flux<T> =
    select(query, T::class.java)

inline fun <reified T: Any> ReactiveCassandraOperations.selectOne(statement: Statement<*>): Mono<T> =
    selectOne(statement, T::class.java)

inline fun <reified T: Any> ReactiveCassandraOperations.selectOne(cql: String): Mono<T> =
    selectOne(cql, T::class.java)

inline fun <reified T: Any> ReactiveCassandraOperations.selectOne(query: Query): Mono<T> =
    selectOne(query, T::class.java)

inline fun <reified T: Any> ReactiveCassandraOperations.slice(statement: Statement<*>): Mono<Slice<T>> =
    slice(statement, T::class.java)

inline fun <reified T: Any> ReactiveCassandraOperations.slice(query: Query): Mono<Slice<T>> =
    slice(query, T::class.java)
