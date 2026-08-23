package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.Resumable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 다중 producer의 값을 단일 consumer에게 비동기로 전달하는 resumable collector입니다.
 *
 * ## 동작/계약
 * - producer admission 수와 terminal 종류를 하나의 atomic 상태로 관리합니다.
 * - 첫 `complete` 또는 `error`만 terminal 상태를 결정하며 이후 signal은 no-op입니다.
 * - terminal보다 먼저 offer commit에 성공한 값은 수락된 값으로 간주하고 enqueue 순서대로 모두 전달합니다.
 * - offer commit 전에 terminal을 관찰한 producer는 값을 enqueue하지 않고 `IllegalStateException`으로 종료합니다.
 * - producer가 mutex 또는 capacity를 기다리는 동안 취소되면 `CancellationException`을 그대로 전파합니다.
 * - collector 실패나 취소는 대기 중 producer를 깨우며, producer에는 cause를 보존한
 *   `CancellationException`을 전파합니다.
 * - `error`가 승리하면 수락된 값을 모두 전달한 뒤 같은 예외를 drain 호출자에게 전파합니다.
 *
 * ```kotlin
 * val rc = BufferedResumableCollector<Int>(4)
 * rc.next(1); rc.next(2); rc.complete()
 * val result = mutableListOf<Int>()
 * rc.drain(collector = FlowCollector { result += it })
 * // result == [1, 2]
 * ```
 */
class BufferedResumableCollector<T> private constructor(
    capacity: Int,
    private val beforeOfferCommit: (suspend () -> Unit)? = null,
    private val afterOffer: (() -> Unit)? = null,
): Resumable() {

    private enum class OfferPhase {
        Idle,
        Preparing,
        Committed,
    }

    private sealed interface Terminal {
        data object Open: Terminal
        data object Complete: Terminal
        data class Error(val cause: Throwable?): Terminal
        data class Cancelled(val cause: Throwable): Terminal
    }

    private data class State(
        val terminal: Terminal,
        val activeAdmissions: Int,
        val offerPhase: OfferPhase = OfferPhase.Idle,
        val pendingTerminal: Terminal? = null,
    )

    companion object: KLoggingChannel() {
        /**
         * 지정한 버퍼 크기로 [BufferedResumableCollector]를 생성합니다.
         *
         * ## 동작/계약
         * - `capacity`가 1 미만이면 1로 보정합니다.
         * - 보정된 크기는 내부 `SpscArrayQueue` 용량으로 사용됩니다.
         *
         * @param capacity 버퍼 슬롯 수입니다. 1 미만이면 1로 보정됩니다.
         */
        @JvmStatic
        operator fun <T> invoke(capacity: Int): BufferedResumableCollector<T> {
            return BufferedResumableCollector(capacity.coerceAtLeast(1))
        }

        internal fun <T> forTest(
            capacity: Int,
            beforeOfferCommit: (suspend () -> Unit)? = null,
            afterOffer: (() -> Unit)? = null,
        ): BufferedResumableCollector<T> = BufferedResumableCollector(
            capacity = capacity.coerceAtLeast(1),
            beforeOfferCommit = beforeOfferCommit,
            afterOffer = afterOffer,
        )
    }

    private val queue: SpscArrayQueue<T> = SpscArrayQueue(capacity)

    private val state = atomic(State(Terminal.Open, 0))

    private val available = atomic(0L)

    private val valueReady = Resumable()
    private val producerMutex = Mutex()

    private val output: Array<Any?> = Array(1) { null }
    private val limit: Int = capacity - (capacity shr 2)

    /**
     * 값을 버퍼에 추가합니다.
     *
     * ## 동작/계약
     * - 버퍼가 가득 찬 동안 소비자가 비울 때까지 suspend 대기합니다.
     * - 성공적으로 적재되면 대기 중 소비자를 깨웁니다.
     * - terminal이 producer보다 먼저 선형화되면 `IllegalStateException`을 던집니다.
     * - mutex 또는 capacity 대기 중 취소되면 `CancellationException`을 그대로 전파합니다.
     * - `Preparing -> Committed` 전이를 값 수락 시점으로 삼으며 이후에는 cancellation을 다시 검사하지 않습니다.
     *
     * @param value 버퍼에 추가할 값입니다.
     */
    suspend fun next(value: T) {
        acquireAdmission()
        try {
            producerMutex.withLock {
                while (true) {
                    currentCoroutineContext().ensureActive()
                    rejectTerminatedProducer()
                    if (!queue.canOffer) {
                        await()
                        continue
                    }

                    beginOfferPreparation()
                    try {
                        beforeOfferCommit?.invoke()
                    } catch (ex: Throwable) {
                        abortOfferPreparation()
                        throw ex
                    }
                    commitOffer()

                    val shouldSignalValue = available.getAndIncrement() == 0L
                    try {
                        check(queue.offer(value)) { "Committed producer offer must have capacity" }
                        afterOffer?.invoke()
                    } finally {
                        finishCommittedOffer()
                        if (shouldSignalValue) {
                            valueReady.resume()
                        }
                    }
                    return@withLock
                }
            }
        } finally {
            releaseAdmission()
        }
    }

    /**
     * 오류 종료를 기록하고 drain 루프를 깨웁니다.
     *
     * ## 동작/계약
     * - 첫 terminal signal인 경우에만 전달받은 예외를 저장합니다.
     * - 버퍼가 모두 비워진 뒤 `drain`에서 저장된 예외가 전파됩니다.
     * - value waiter와 capacity waiter를 모두 깨웁니다.
     *
     * @param ex 종료 시 전파할 예외입니다.
     */
    fun error(ex: Throwable?) {
        terminate(Terminal.Error(ex))
    }

    /**
     * 정상 종료를 기록하고 drain 루프를 깨웁니다.
     *
     * ## 동작/계약
     * - 첫 terminal signal인 경우에만 정상 종료 상태로 전환합니다.
     * - 버퍼에 남은 값을 모두 전달한 뒤 `drain`이 정상 종료됩니다.
     * - value waiter와 capacity waiter를 모두 깨웁니다.
     */
    fun complete() {
        terminate(Terminal.Complete)
    }

    /**
     * 버퍼의 값을 collector로 비우고 완료 또는 오류까지 처리합니다.
     *
     * ## 동작/계약
     * - 버퍼에 값이 있으면 즉시 `collector.emit`을 호출하고, 없으면 `valueReady.await()`로 대기합니다.
     * - `collector.emit`이 예외를 던지면 `onCrash`를 호출하고 취소 상태로 전환한 뒤 예외를 다시 던집니다.
     * - terminal 상태이고 버퍼가 비었으며 active admission이 0일 때만 종료합니다.
     * - terminal error가 있으면 buffered 값을 모두 전달한 뒤 같은 예외를 전파합니다.
     *
     * ```kotlin
     * val rc = BufferedResumableCollector<Int>(2)
     * rc.next(10); rc.complete()
     * val result = mutableListOf<Int>()
     * rc.drain(collector = FlowCollector { result += it })
     * // result == [10]
     * ```
     *
     * @param collector 버퍼에서 꺼낸 값을 소비할 collector입니다.
     * @param onCrash `collector.emit` 실패 시 호출할 콜백입니다.
     */
    suspend fun drain(
        collector: FlowCollector<T>,
        onCrash: ((BufferedResumableCollector<T>) -> Unit)? = null,
    ) {
        var consumed = 0L
        val limit = this.limit.toLong()

        while (true) {
            ensureDrainActive()
            val ne = !queue.poll(output)
            val currentState = state.value

            if (ne && currentState.terminal !== Terminal.Open && currentState.activeAdmissions == 0) {
                when (val terminal = currentState.terminal) {
                    Terminal.Open -> kotlin.error("Open state cannot terminate drain")
                    Terminal.Complete -> break
                    is Terminal.Error -> terminal.cause?.let { throw it } ?: break
                    is Terminal.Cancelled -> throw terminal.cause
                }
            }

            if (!ne) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    collector.emit(output[0] as T)
                } catch (ex: Throwable) {
                    onCrash?.invoke(this)
                    cancelDrain(ex)
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
                try {
                    valueReady.await()
                } catch (ex: Throwable) {
                    cancelDrain(ex)
                    throw ex
                }
            }
            consumed = 0L
        }
    }

    private suspend fun acquireAdmission() {
        while (true) {
            currentCoroutineContext().ensureActive()
            val current = state.value
            val terminal = current.pendingTerminal ?: current.terminal
            if (terminal !== Terminal.Open) {
                throw producerFailure(terminal)
            }
            val updated = current.copy(activeAdmissions = current.activeAdmissions + 1)
            if (state.compareAndSet(current, updated)) {
                return
            }
        }
    }

    private fun releaseAdmission() {
        while (true) {
            val current = state.value
            check(current.activeAdmissions > 0) { "Active producer admission underflow" }
            val updated = current.copy(activeAdmissions = current.activeAdmissions - 1)
            if (state.compareAndSet(current, updated)) {
                if (updated.terminal !== Terminal.Open && updated.activeAdmissions == 0) {
                    valueReady.resume()
                }
                return
            }
        }
    }

    private fun rejectTerminatedProducer() {
        val current = state.value
        val terminal = current.pendingTerminal ?: current.terminal
        if (terminal !== Terminal.Open) {
            throw producerFailure(terminal)
        }
    }

    private fun beginOfferPreparation() {
        while (true) {
            val current = state.value
            val terminal = current.pendingTerminal ?: current.terminal
            if (terminal !== Terminal.Open) {
                throw producerFailure(terminal)
            }
            check(current.offerPhase == OfferPhase.Idle) { "Concurrent producer offer preparation" }
            if (state.compareAndSet(current, current.copy(offerPhase = OfferPhase.Preparing))) {
                return
            }
        }
    }

    private fun abortOfferPreparation() {
        while (true) {
            val current = state.value
            check(current.offerPhase == OfferPhase.Preparing) { "Producer offer preparation is not active" }
            if (state.compareAndSet(current, current.copy(offerPhase = OfferPhase.Idle))) {
                return
            }
        }
    }

    private fun commitOffer() {
        while (true) {
            val current = state.value
            check(current.offerPhase == OfferPhase.Preparing) { "Producer offer preparation is not active" }
            val terminal = current.pendingTerminal ?: current.terminal
            if (terminal !== Terminal.Open) {
                if (state.compareAndSet(current, current.copy(offerPhase = OfferPhase.Idle))) {
                    throw producerFailure(terminal)
                }
                continue
            }
            if (state.compareAndSet(current, current.copy(offerPhase = OfferPhase.Committed))) {
                return
            }
        }
    }

    private fun finishCommittedOffer() {
        while (true) {
            val current = state.value
            check(current.offerPhase == OfferPhase.Committed) { "Producer offer is not committed" }
            val terminal = current.pendingTerminal ?: current.terminal
            val updated = current.copy(
                terminal = terminal,
                offerPhase = OfferPhase.Idle,
                pendingTerminal = null,
            )
            if (state.compareAndSet(current, updated)) {
                if (terminal !== Terminal.Open) {
                    signalTerminalWaiters()
                }
                return
            }
        }
    }

    private fun producerFailure(terminal: Terminal): Throwable = when (terminal) {
        Terminal.Open -> kotlin.error("Open collector cannot reject a producer")
        Terminal.Complete,
        is Terminal.Error,
        -> IllegalStateException("BufferedResumableCollector is already terminated.")
        is Terminal.Cancelled -> terminal.cause.asProducerCancellation()
    }

    private fun Throwable.asProducerCancellation(): CancellationException {
        if (this is CancellationException) {
            return this
        }
        return CancellationException("Collector drain failed.").also { it.initCause(this) }
    }

    private fun terminate(terminal: Terminal) {
        while (true) {
            val current = state.value
            if (current.terminal !== Terminal.Open || current.pendingTerminal != null) {
                return
            }
            val updated = when (current.offerPhase) {
                OfferPhase.Idle,
                OfferPhase.Preparing,
                -> current.copy(terminal = terminal)
                OfferPhase.Committed -> current.copy(pendingTerminal = terminal)
            }
            if (state.compareAndSet(current, updated)) {
                if (current.offerPhase != OfferPhase.Committed) {
                    signalTerminalWaiters()
                }
                return
            }
        }
    }

    private suspend fun ensureDrainActive() {
        try {
            currentCoroutineContext().ensureActive()
        } catch (ex: CancellationException) {
            cancelDrain(ex)
            throw ex
        }
    }

    private fun signalTerminalWaiters() {
        valueReady.resume()
        resume()
    }

    private fun cancelDrain(cause: Throwable) {
        terminate(Terminal.Cancelled(cause))
    }
}
