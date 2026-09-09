package io.bluetape4k.workflow.coroutines

import kotlinx.coroutines.CancellationException

/**
 * suspend 블록에서 발생한 일반 예외만 [Result]로 변환합니다.
 * 취소와 [Error]는 호출자에게 전파하여 코루틴 생명주기와 치명적 오류를 보존합니다.
 */
@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> suspendResult(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
