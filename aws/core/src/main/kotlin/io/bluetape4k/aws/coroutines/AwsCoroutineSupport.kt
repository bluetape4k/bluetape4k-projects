package io.bluetape4k.aws.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.newCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * AWS 동기 함수 호출을 suspend 함수로 호출하게 한다.
 *
 * @param context CoroutineContext
 * @param method 실행할 메소드
 * @return 메소드 실행 결과
 */
suspend inline fun <RES: Any> suspendCommand(
    context: CoroutineContext = EmptyCoroutineContext,
    crossinline method: () -> RES,
): RES = coroutineScope {
    withContext(newCoroutineContext(context + Dispatchers.IO)) {
        method()
    }
}

/**
 * AWS 동기 함수 호출을 suspend 함수로 호출하게 한다.
 *
 * @param context CoroutineContext
 * @param request [method] 의 입력 정보
 * @param method 실행할 메소드
 * @return 메소드 실행 결과
 */
suspend inline fun <REQ, RES: Any> suspendCommand(
    context: CoroutineContext = EmptyCoroutineContext,
    request: REQ,
    crossinline method: (request: REQ) -> RES,
): RES = coroutineScope {
    withContext(newCoroutineContext(context + Dispatchers.IO)) {
        method(request)
    }
}
