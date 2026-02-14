package io.bluetape4k.io.okio.coroutines.internal

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Okio 코루틴에서 `await` 함수를 제공합니다.
 */
internal suspend inline fun SelectableChannel.await(ops: Int) {
    selector.waitForSelection(this, ops)
}

internal val selector: SelectorThread by lazy {
    SelectorThread().apply {
        start()
    }
}

/**
 * Okio 코루틴에서 사용하는 `SelectorThread` 타입입니다.
 */
internal class SelectorThread: Thread("okio selector") {

    companion object: KLogging()

    init {
        isDaemon = true
    }

    private val selector = Selector.open()
    private val keys = ConcurrentLinkedQueue<SelectionKey>()

    /**
     * Okio 코루틴에서 `waitForSelection` 함수를 제공합니다.
     */
    suspend fun waitForSelection(channel: SelectableChannel, ops: Int) {
        suspendCancellableCoroutine<Unit> { cont ->
            val key = channel.register(selector, ops, cont)
            check(key.attachment() === cont) { "already registered" }

            cont.invokeOnCancellation {
                key.cancel()
                selector.wakeup()
            }

            keys.add(key)
            selector.wakeup()
        }
    }

    /**
     * Okio 코루틴에서 `run` 함수를 제공합니다.
     */
    @Suppress("UNCHECKED_CAST")
    override fun run() {
        while (true) {
            try {
                selector.select()
                selector.selectedKeys().clear()

                val pendingKeys = ArrayList<SelectionKey>(keys.size)
                while (true) {
                    val key = keys.poll() ?: break
                    pendingKeys.add(key)
                }

                for (key in pendingKeys) {
                    val cont = key.attachment() as? CancellableContinuation<Unit> ?: continue

                    try {
                        if (!key.isValid) {
                            key.attach(null)
                            if (!cont.isCompleted) cont.resumeWithException(IOException("closed"))
                        } else if ((key.readyOps() and key.interestOps()) != 0) {
                            key.attach(null)
                            if (!cont.isCompleted) cont.resume(Unit)
                        } else {
                            keys.add(key)
                        }
                    } catch (e: Throwable) {
                        key.attach(null)
                        if (!cont.isCompleted) cont.resumeWithException(IOException("closed", e))
                    }
                }
            } catch (e: Throwable) {
                log.error(e) { "Error in SelectorThread" }
            }
        }
    }
}
