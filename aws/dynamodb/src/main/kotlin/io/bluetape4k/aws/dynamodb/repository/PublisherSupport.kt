package io.bluetape4k.aws.dynamodb.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.awaitFirst
import software.amazon.awssdk.core.async.SdkPublisher
import software.amazon.awssdk.enhanced.dynamodb.model.Page
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher

/**
 * 첫 번째 페이지의 아이템을 반환하는 Flow를 생성합니다.
 */
fun <T: Any> SdkPublisher<Page<T>>.findFirst(): Flow<T> = flow {
    emitAll(awaitFirst().items().asFlow())
}

/**
 * 첫 번째 페이지의 아이템을 반환하는 Flow를 생성합니다.
 */
fun <T: Any> PagePublisher<T>.findFirst(): Flow<T> = flow {
    emitAll(awaitFirst().items().asFlow())
}

/**
 * 모든 페이지의 아이템의 수를 반환합니다.
 */
suspend fun <T: Any> SdkPublisher<Page<T>>.count(): Long {
    return awaitFirst().items().count().toLong()
}

/**
 * 모든 페이지의 아이템의 수를 반환합니다.
 */
suspend fun <T: Any> PagePublisher<T>.count(): Long {
    return awaitFirst().items().count().toLong()
}