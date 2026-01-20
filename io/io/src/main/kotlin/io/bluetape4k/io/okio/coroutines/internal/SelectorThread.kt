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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend inline fun SelectableChannel.await(ops: Int) {
    selector.waitForSelection(this, ops)
}

internal val selector: SelectorThread by lazy {
    SelectorThread().apply {
        start()
    }
}

internal class SelectorThread: Thread("okio selector") {

    companion object: KLogging()

    init {
        isDaemon = true
    }

    private val selector = Selector.open()
    private val keys = ConcurrentLinkedQueue<SelectionKey>()
    private val lock = ReentrantLock()

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

    @Suppress("UNCHECKED_CAST")
    override fun run() {
        while (true) {
            try {
                lock.withLock {
                    val selectedKeys = selector.selectedKeys()
                    val keyLock = ReentrantLock()
                    keyLock.withLock {
                        selector.select()
                        selectedKeys.clear()

                        val iter = keys.iterator()
                        while (iter.hasNext()) {
                            val key = iter.next()

                            if (!key.isValid) {
                                val cont = key.attach(null) as CancellableContinuation<Unit>
                                if (!cont.isCompleted) cont.resumeWithException(IOException("closed"))
                                iter.remove()
                            } else if (key.readyOps() > 0) {
                                val cont = key.attach(null) as CancellableContinuation<Unit>
                                cont.resume(Unit)
                                iter.remove()
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                // log error 
                log.error(e) { "Error in SelectorThread" }
            }
        }
    }
}
