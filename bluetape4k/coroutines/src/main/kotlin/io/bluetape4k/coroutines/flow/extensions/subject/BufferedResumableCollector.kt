package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.FlowCollector


/**
 * `capacity` 만큼 버퍼렁을 할 수 있는 [FlowCollector]
 *
 * ```
 * @Test
 * fun `capacity 만큼 버퍼링을 합니다`() = runTest {
 *     val bc = BufferedResumableCollector<Int>(32)
 *     val n = 10_000
 *     val counter = AtomicInteger(0)
 *     withSingleThread { dispatcher ->
 *         val job = launch(dispatcher) {
 *             repeat(n) {
 *                 bc.next(it)
 *             }
 *             bc.complete()
 *         }.log("job")
 *         yield()
 *         val collector = FlowCollector<Int> {
 *             counter.incrementAndGet()
 *         }
 *         bc.drain(collector)
 *         job.join()
 *     }
 *     counter.get() shouldBeEqualTo n
 * }
 * ```
 *
 * @param capacity 버퍼 크기
 */
class BufferedResumableCollector<T> private constructor(capacity: Int): Resumable() {

    companion object: KLoggingChannel() {
        @JvmStatic
        operator fun <T> invoke(capacity: Int): BufferedResumableCollector<T> {
            return BufferedResumableCollector(capacity.coerceAtLeast(1))
        }
    }

    private val queue: SpscArrayQueue<T> = SpscArrayQueue(capacity)

    private val done = atomic(false)
    private val cancelled = atomic(false)

    @Volatile
    private var error: Throwable? = null

    private val available = atomic(0L)

    private val valueReady = Resumable()

    private val output: Array<Any?> = Array(1) { null }
    private val limit: Int = capacity - (capacity shr 2)

    suspend fun next(value: T) {
        while (!cancelled.value) {
            if (queue.offer(value)) {
                if (available.getAndIncrement() == 0L) {
                    valueReady.resume()
                }
                break
            }
            await()
        }
        if (cancelled.value) {
            throw CancellationException("Cancel in next.")
        }
    }

    fun error(ex: Throwable?) {
        error = ex
        done.value = true
        valueReady.resume()
    }

    fun complete() {
        done.value = true
        valueReady.resume()
    }

    suspend fun drain(
        collector: FlowCollector<T>,
        onCrash: ((BufferedResumableCollector<T>) -> Unit)? = null,
    ) {
        var consumed = 0L
        val limit = this.limit.toLong()

        while (true) {
            val ne = !queue.poll(output)

            if (done.value && ne) {
                error?.let { throw it }
                break
            }

            // item exists in buffer
            if (!ne) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    collector.emit(output[0] as T)
                } catch (ex: Throwable) {
                    onCrash?.invoke(this)
                    cancelled.value = true
                    resume()

                    throw ex
                }

                if (consumed++ == limit) {
                    available.addAndGet(-consumed)
                    consumed = 0L
                    resume()
                }

                continue
            }

            if (available.addAndGet(-consumed) == 0L) {
                resume()
                valueReady.await()
            }
            consumed = 0L
        }
    }
}
