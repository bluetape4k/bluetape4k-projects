package io.bluetape4k.spring.cassandra

import com.datastax.oss.driver.api.core.cql.Statement
import org.springframework.data.cassandra.core.ReactiveCassandraOperations
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.domain.Slice
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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
