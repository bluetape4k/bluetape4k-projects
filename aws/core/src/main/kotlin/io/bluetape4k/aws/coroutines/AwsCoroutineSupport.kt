package io.bluetape4k.aws.coroutines

import io.bluetape4k.coroutines.support.getOrCurrent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 인자가 없는 AWS 동기 호출을 `Dispatchers.IO` 기반 suspend 호출로 감쌉니다.
 *
 * ## 동작/계약
 * - `context.getOrCurrent() + Dispatchers.IO` 문맥에서 [method]를 실행한다.
 * - [context]가 `EmptyCoroutineContext`면 현재 컨텍스트를 기준으로 IO 디스패처를 추가한다.
 *
 * ```kotlin
 * suspend fun loadBuckets(s3: software.amazon.awssdk.services.s3.S3Client): Int =
 *     suspendCommand { s3.listBuckets().buckets().size }
 * // result == 버킷 개수(Int)
 * ```
 */
suspend inline fun <RES: Any> suspendCommand(
    context: CoroutineContext = EmptyCoroutineContext,
    crossinline method: () -> RES,
): RES = withContext(context.getOrCurrent() + Dispatchers.IO) {
    method()
}

/**
 * 요청 객체를 받는 AWS 동기 호출을 `Dispatchers.IO` 기반 suspend 호출로 감쌉니다.
 *
 * ## 동작/계약
 * - `context.getOrCurrent() + Dispatchers.IO`에서 [method]에 [request]를 전달해 실행한다.
 * - [method]가 던진 예외는 감추지 않고 호출자에게 그대로 전파된다.
 *
 * ```kotlin
 * suspend fun loadObject(
 *     s3: software.amazon.awssdk.services.s3.S3Client,
 *     request: software.amazon.awssdk.services.s3.model.GetObjectRequest,
 * ) = suspendCommand(request = request, method = s3::getObjectAsBytes)
 * // result == ResponseBytes<GetObjectResponse>
 * ```
 */
suspend inline fun <REQ, RES: Any> suspendCommand(
    context: CoroutineContext = EmptyCoroutineContext,
    request: REQ,
    crossinline method: (request: REQ) -> RES,
): RES = withContext(context.getOrCurrent() + Dispatchers.IO) {
    method(request)
}
