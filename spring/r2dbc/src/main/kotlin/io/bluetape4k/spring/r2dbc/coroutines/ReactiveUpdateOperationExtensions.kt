package io.bluetape4k.spring.r2dbc.coroutines

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.r2dbc.core.ReactiveUpdateOperation
import org.springframework.data.r2dbc.core.update
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update

suspend inline fun <reified T: Any> ReactiveUpdateOperation.suspendUpdate(query: Query, update: Update): Long =
    update<T>().matching(query).apply(update).awaitSingle()
