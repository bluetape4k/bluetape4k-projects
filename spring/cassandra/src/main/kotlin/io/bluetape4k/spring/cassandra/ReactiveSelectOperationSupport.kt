package io.bluetape4k.spring.cassandra

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.cassandra.core.ReactiveSelectOperation
import org.springframework.data.cassandra.core.ReactiveSelectOperation.SelectWithProjection
import org.springframework.data.cassandra.core.ReactiveSelectOperation.SelectWithQuery

inline fun <reified R: Any> SelectWithProjection<*>.cast(): SelectWithQuery<R> =
    `as`(R::class.java)


suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.suspendCount(): Long =
    count().awaitSingle()

suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.suspendExists(): Boolean =
    exists().awaitSingle()

suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.first(): T =
    first().awaitSingle()

suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.suspendOne(): T? =
    one().awaitSingle()

suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.suspendAll(): List<T> =
    all().collectList().awaitSingle()
