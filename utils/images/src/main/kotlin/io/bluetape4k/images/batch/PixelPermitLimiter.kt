package io.bluetape4k.images.batch

import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PixelPermitLimiter(
    private val maxPixels: Long,
) {
    private val mutex = Mutex()
    private var availablePixels = maxPixels

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
            val acquired = mutex.withLock {
                if (availablePixels >= permits) {
                    availablePixels -= permits
                    true
                } else {
                    false
                }
            }

            if (acquired) {
                return
            }

            delay(PIXEL_PERMIT_RETRY_DELAY_MILLIS)
        }
    }

    private suspend fun release(permits: Long) {
        mutex.withLock {
            availablePixels = (availablePixels + permits).coerceAtMost(maxPixels)
        }
    }
}
