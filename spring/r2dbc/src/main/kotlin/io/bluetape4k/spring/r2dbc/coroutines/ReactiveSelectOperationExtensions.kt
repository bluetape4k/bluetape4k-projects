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

suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitExists(query: Query): Boolean =
    select<T>().matching(query).awaitExists()

@Deprecated("Use awaitExists instead", ReplaceWith("awaitExists(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coExists(query: Query): Boolean =
    select<T>().matching(query).awaitExists()


suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitCount(query: Query): Long =
    select<T>().matching(query).awaitCount()

@Deprecated("Use awaitCount instead", ReplaceWith("awaitCount(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coCount(query: Query): Long =
    select<T>().matching(query).awaitCount()

suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitCountAll(): Long =
    awaitCount<T>(Query.empty())

@Deprecated("Use awaitCountAll instead", ReplaceWith("awaitCountAll()"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coCountAll(): Long =
    coCount<T>(Query.empty())

inline fun <reified T: Any> R2dbcEntityOperations.awaitSelect(query: Query): Flow<T> =
    select<T>().matching(query).flow()

@Deprecated("Use awaitSelect instead", ReplaceWith("awaitSelect(query)"))
inline fun <reified T: Any> R2dbcEntityOperations.coSelect(query: Query): Flow<T> =
    select<T>().matching(query).flow()

inline fun <reified T: Any> R2dbcEntityOperations.awaitSelectAll(): Flow<T> =
    awaitSelect(Query.empty())

@Deprecated("Use awaitSelectAll instead", ReplaceWith("awaitSelectAll()"))
inline fun <reified T: Any> R2dbcEntityOperations.coSelectAll(): Flow<T> =
    coSelect(Query.empty())

suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitSelectOne(query: Query): T =
    select<T>().matching(query).one().awaitSingle()

@Deprecated("Use awaitSelectOne instead", ReplaceWith("awaitSelectOne(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coSelectOne(query: Query): T =
    select<T>().matching(query).one().awaitSingle()

suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitSelectOneOrNull(query: Query): T? =
    select<T>().matching(query).one().awaitSingleOrNull()

@Deprecated("Use awaitSelectOneOrNull instead", ReplaceWith("awaitSelectOneOrNull(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coSelectOneOrNull(query: Query): T? =
    select<T>().matching(query).one().awaitSingleOrNull()

suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitSelectFirst(query: Query): T =
    select<T>().matching(query).first().awaitSingle()

@Deprecated("Use awaitSelectFirst instead", ReplaceWith("awaitSelectFirst(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coSelectFirst(query: Query): T =
    select<T>().matching(query).first().awaitSingle()

suspend inline fun <reified T: Any> R2dbcEntityOperations.awaitSelectFirstOrNull(query: Query): T? =
    select<T>().matching(query).first().awaitSingleOrNull()

@Deprecated("Use awaitSelectFirstOrNull instead", ReplaceWith("awaitSelectFirstOrNull(query)"))
suspend inline fun <reified T: Any> R2dbcEntityOperations.coSelectFirstOrNull(query: Query): T? =
    select<T>().matching(query).first().awaitSingleOrNull()
