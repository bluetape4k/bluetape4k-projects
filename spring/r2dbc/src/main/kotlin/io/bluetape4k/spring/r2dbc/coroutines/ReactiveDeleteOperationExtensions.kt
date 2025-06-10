package io.bluetape4k.spring.r2dbc.coroutines

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.r2dbc.core.ReactiveDeleteOperation
import org.springframework.data.r2dbc.core.delete
import org.springframework.data.relational.core.query.Query

suspend inline fun <reified T: Any> ReactiveDeleteOperation.suspendDelete(query: Query): Long =
    delete<T>().matching(query).all().awaitSingle()

@Deprecated("Use `suspendDelete` instead", ReplaceWith("suspendDelete(query)"))
suspend inline fun <reified T: Any> ReactiveDeleteOperation.coDelete(query: Query): Long =
    delete<T>().matching(query).all().awaitSingle()

suspend inline fun <reified T: Any> ReactiveDeleteOperation.suspendDeleteAll(): Long =
    delete<T>().matching(Query.empty()).all().awaitSingle()

@Deprecated("Use `suspendDeleteAll` instead", ReplaceWith("suspendDeleteAll()"))
suspend inline fun <reified T: Any> ReactiveDeleteOperation.coDeleteAll(): Long =
    delete<T>().matching(Query.empty()).all().awaitSingle()
