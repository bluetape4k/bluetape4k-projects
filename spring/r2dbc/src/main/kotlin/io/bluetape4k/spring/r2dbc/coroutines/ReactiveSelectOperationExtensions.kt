package io.bluetape4k.spring.r2dbc.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.r2dbc.core.awaitCount
import org.springframework.data.r2dbc.core.awaitExists
import org.springframework.data.r2dbc.core.flow
import org.springframework.data.r2dbc.core.select
import org.springframework.data.relational.core.query.Query

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendExists(query: Query): Boolean =
    select<T>().matching(query).awaitExists()

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendCount(query: Query): Long =
    select<T>().matching(query).awaitCount()

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendCountAll(): Long =
    suspendCount<T>(Query.empty())

inline fun <reified T: Any> R2dbcEntityOperations.suspendSelect(query: Query): Flow<T> =
    select<T>().matching(query).flow()


inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectAll(): Flow<T> =
    suspendSelect(Query.empty())


suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectOne(query: Query): T =
    select<T>().matching(query).one().awaitSingle()

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectOneOrNull(query: Query): T? =
    select<T>().matching(query).one().awaitSingleOrNull()

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectFirst(query: Query): T =
    select<T>().matching(query).first().awaitSingle()

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectFirstOrNull(query: Query): T? =
    select<T>().matching(query).first().awaitSingleOrNull()
