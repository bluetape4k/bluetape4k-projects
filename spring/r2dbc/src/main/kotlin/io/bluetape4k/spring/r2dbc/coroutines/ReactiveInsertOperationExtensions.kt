package io.bluetape4k.spring.r2dbc.coroutines

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.r2dbc.core.ReactiveInsertOperation
import org.springframework.data.r2dbc.core.insert

suspend inline fun <reified T: Any> ReactiveInsertOperation.coInsert(entity: T): T =
    insert<T>().using(entity).awaitSingle()


suspend inline fun <reified T: Any> ReactiveInsertOperation.coInsertOrNull(entity: T): T? =
    insert<T>().using(entity).awaitSingleOrNull()
