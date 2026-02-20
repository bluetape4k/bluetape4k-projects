package io.bluetape4k.spring.cassandra

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.cassandra.core.ReactiveSelectOperation
import org.springframework.data.cassandra.core.ReactiveSelectOperation.SelectWithProjection
import org.springframework.data.cassandra.core.ReactiveSelectOperation.SelectWithQuery

inline fun <reified R: Any> SelectWithProjection<*>.cast(): SelectWithQuery<R> =
    `as`(R::class.java)


/**
 * 조회 결과의 건수를 반환합니다.
 */
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.countSuspending(): Long =
    count().awaitSingle()

@Deprecated(
    message = "countSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("countSuspending()")
)
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.suspendCount(): Long =
    countSuspending()

/**
 * 조회 결과의 존재 여부를 반환합니다.
 */
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.existsSuspending(): Boolean =
    exists().awaitSingle()

@Deprecated(
    message = "existsSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("existsSuspending()")
)
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.suspendExists(): Boolean =
    existsSuspending()

suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.first(): T =
    first().awaitSingle()

/**
 * 단건을 조회하고 없으면 null을 반환합니다.
 */
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.oneSuspending(): T? =
    one().awaitSingle()

@Deprecated(
    message = "oneSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("oneSuspending()")
)
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.suspendOne(): T? =
    oneSuspending()

/**
 * 전체 결과를 리스트로 반환합니다.
 */
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.allSuspending(): List<T> =
    all().collectList().awaitSingle()

@Deprecated(
    message = "allSuspending으로 대체되었습니다.",
    replaceWith = ReplaceWith("allSuspending()")
)
suspend fun <T: Any> ReactiveSelectOperation.TerminatingSelect<T>.suspendAll(): List<T> =
    allSuspending()
