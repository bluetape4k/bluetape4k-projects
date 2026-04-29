package io.bluetape4k.images.batch

import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 동시에 처리 중인 이미지의 픽셀 총량을 제한하는 가중 세마포어입니다.
 *
 * 픽셀 슬롯이 해제될 때까지 대기 코루틴은 실제로 suspend 상태로 진입하며,
 * 폴링 루프를 사용하지 않습니다.
 */
internal class PixelPermitLimiter(
    private val maxPixels: Long,
) {
    private val mutex = Mutex()
    private var availablePixels = maxPixels
    private val waiters = ArrayDeque<CompletableDeferred<Unit>>()

    init {
        maxPixels.requirePositiveNumber("maxPixels")
    }

    suspend fun <T> withPermit(pixelCount: Long, block: suspend () -> T): T {
        val permits = pixelCount.coerceIn(MIN_PIXEL_PERMIT, maxPixels)
        acquire(permits)
        try {
            return block()
        } finally {
            release(permits)
        }
    }

    private suspend fun acquire(permits: Long) {
        while (true) {
            val deferred = mutex.withLock {
                if (availablePixels >= permits) {
                    availablePixels -= permits
                    return
                }
                CompletableDeferred<Unit>().also { waiters.addLast(it) }
            }
            try {
                deferred.await()
            } catch (e: CancellationException) {
                mutex.withLock { waiters.remove(deferred) }
                throw e
            }
        }
    }

    private suspend fun release(permits: Long) {
        val toWake: List<CompletableDeferred<Unit>>
        mutex.withLock {
            availablePixels = (availablePixels + permits).coerceAtMost(maxPixels)
            toWake = waiters.toList()
            waiters.clear()
        }
        toWake.forEach { it.complete(Unit) }
    }
}
