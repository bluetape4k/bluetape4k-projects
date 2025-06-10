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

@Deprecated("Use suspendExists instead", ReplaceWith("suspendExists(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coExists(query: Query): Boolean =
    select<T>().matching(query).awaitExists()


suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendCount(query: Query): Long =
    select<T>().matching(query).awaitCount()

@Deprecated("Use suspendCount instead", ReplaceWith("suspendCount(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coCount(query: Query): Long =
    select<T>().matching(query).awaitCount()

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendCountAll(): Long =
    suspendCount<T>(Query.empty())

@Deprecated("Use suspendCountAll instead", ReplaceWith("suspendCountAll()"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coCountAll(): Long =
    coCount<T>(Query.empty())

inline fun <reified T: Any> R2dbcEntityOperations.suspendSelect(query: Query): Flow<T> =
    select<T>().matching(query).flow()

@Deprecated("Use suspendSelect instead", ReplaceWith("suspendSelect(query)"))
inline fun <reified T: Any> R2dbcEntityOperations.coSelect(query: Query): Flow<T> =
    select<T>().matching(query).flow()

inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectAll(): Flow<T> =
    suspendSelect(Query.empty())

@Deprecated("Use suspendSelectAll instead", ReplaceWith("suspendSelectAll()"))
inline fun <reified T: Any> R2dbcEntityOperations.coSelectAll(): Flow<T> =
    coSelect(Query.empty())

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectOne(query: Query): T =
    select<T>().matching(query).one().awaitSingle()

@Deprecated("Use suspendSelectOne instead", ReplaceWith("suspendSelectOne(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coSelectOne(query: Query): T =
    select<T>().matching(query).one().awaitSingle()

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectOneOrNull(query: Query): T? =
    select<T>().matching(query).one().awaitSingleOrNull()

@Deprecated("Use suspendSelectOneOrNull instead", ReplaceWith("suspendSelectOneOrNull(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coSelectOneOrNull(query: Query): T? =
    select<T>().matching(query).one().awaitSingleOrNull()

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectFirst(query: Query): T =
    select<T>().matching(query).first().awaitSingle()

@Deprecated("Use suspendSelectFirst instead", ReplaceWith("suspendSelectFirst(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coSelectFirst(query: Query): T =
    select<T>().matching(query).first().awaitSingle()

suspend inline fun <reified T: Any> R2dbcEntityOperations.suspendSelectFirstOrNull(query: Query): T? =
    select<T>().matching(query).first().awaitSingleOrNull()

@Deprecated("Use suspendSelectFirstOrNull instead", ReplaceWith("suspendSelectFirstOrNull(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coSelectFirstOrNull(query: Query): T? =
    select<T>().matching(query).first().awaitSingleOrNull()
