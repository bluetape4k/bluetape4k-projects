package io.bluetape4k.tink.keyset.redis

/**
 * 작업 실패와 lock cleanup 실패를 함께 관측 가능하게 유지합니다.
 *
 * 작업이 성공하면 cleanup 예외를 호출자에게 전달하고, 작업도 실패한 경우에는 작업 예외를
 * 주 예외로 유지하면서 cleanup 예외를 suppressed 예외로 추가합니다.
 */
internal inline fun <T> withObservedCleanup(
    action: () -> T,
    cleanup: () -> Unit,
): T {
    var actionFailure: Throwable? = null
    return try {
        action()
    } catch (failure: Throwable) {
        actionFailure = failure
        throw failure
    } finally {
        try {
            cleanup()
        } catch (cleanupFailure: Throwable) {
            actionFailure?.addSuppressed(cleanupFailure) ?: throw cleanupFailure
        }
    }
}
