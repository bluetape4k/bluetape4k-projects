package io.bluetape4k.aws.dynamodb.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.asFlow
import software.amazon.awssdk.core.async.SdkPublisher
import software.amazon.awssdk.enhanced.dynamodb.model.Page
import software.amazon.awssdk.enhanced.dynamodb.model.PagePublisher

/**
 * 첫 번째 페이지의 아이템을 반환하는 Flow를 생성합니다.
 */
suspend fun <T: Any> SdkPublisher<Page<T>>.findFirst(): List<T> =
    asFlow().firstOrNull()?.items() ?: emptyList()

/**
 * 첫 번째 페이지의 아이템을 반환하는 Flow를 생성합니다.
 */
suspend fun <T: Any> PagePublisher<T>.findFirst(): List<T> =
    asFlow().firstOrNull()?.items() ?: emptyList()

/**
 * 모든 페이지의 아이템의 수를 반환합니다.
 */
suspend fun <T: Any> SdkPublisher<Page<T>>.count(): Long =
    asFlow().first().items().count().toLong()

/**
 * 모든 페이지의 아이템의 수를 반환합니다.
 */
suspend fun <T: Any> PagePublisher<T>.count(): Long =
    asFlow().first().items().count().toLong()
