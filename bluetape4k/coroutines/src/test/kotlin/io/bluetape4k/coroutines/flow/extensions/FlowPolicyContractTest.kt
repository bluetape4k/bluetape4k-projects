package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class FlowPolicyContractTest: AbstractFlowTest() {

    @Test
    fun concatIsFailFastAndSkipsLaterSources() = runTest {
        val secondCollected = AtomicBoolean(false)
        val failure = IllegalStateException("first")

        val actual = assertFailsWith<IllegalStateException> {
            concat(
                flow {
                    emit(1)
                    throw failure
                },
                flow {
                    secondCollected.set(true)
                    emit(2)
                },
            ).toList()
        }

        actual::class shouldBeEqualTo failure::class
        actual.message shouldBeEqualTo failure.message
        secondCollected.get().shouldBeFalse()
    }

    @Test
    fun mergeFailureCancelsSiblingAndPreservesOriginalFailure() = runTest {
        val siblingCancelled = CompletableDeferred<Unit>()
        val failure = IllegalStateException("merge")

        val actual = assertFailsWith<IllegalStateException> {
            merge(
                flow {
                    try {
                        awaitCancellation()
                    } finally {
                        siblingCancelled.complete(Unit)
                    }
                },
                flow<Int> { throw failure },
            ).collect()
        }

        actual::class shouldBeEqualTo failure::class
        actual.message shouldBeEqualTo failure.message
        siblingCancelled.await()
    }

    @Test
    fun bufferSuspendKeepsAtMostOnePendingValue() = runTest {
        val releaseCollector = CompletableDeferred<Unit>()
        val thirdEmit = CompletableDeferred<Unit>()

        val job = launch {
            flow {
                emit(1)
                emit(2)
                emit(3)
                thirdEmit.complete(Unit)
            }.buffer(capacity = 1, onBufferOverflow = BufferOverflow.SUSPEND)
                .collect {
                    if (it == 1) releaseCollector.await()
                }
        }

        runCurrent()
        thirdEmit.isCompleted.shouldBeFalse()
        releaseCollector.complete(Unit)
        job.join()
        thirdEmit.isCompleted.shouldBeTrue()
    }

    @Test
    fun conflateKeepsLatestWhileCollectorIsSuspended() = runTest {
        val releaseCollector = CompletableDeferred<Unit>()
        val result = mutableListOf<Int>()

        val job = launch {
            flowOf(1, 2, 3).conflate().collect {
                result += it
                if (it == 1) releaseCollector.await()
            }
        }

        runCurrent()
        releaseCollector.complete(Unit)
        job.join()
        result shouldBeEqualTo listOf(1, 3)
    }

    @Test
    fun callerCancellationIsNotConvertedToDataError() = runTest {
        val upstreamCancelled = AtomicBoolean(false)
        val job = launch {
            flow<Int> {
                try {
                    awaitCancellation()
                } finally {
                    upstreamCancelled.set(true)
                }
            }.collect()
        }

        runCurrent()
        job.cancelAndJoin()
        job.isCancelled.shouldBeTrue()
        upstreamCancelled.get().shouldBeTrue()
    }

    @Test
    fun flowCatchDoesNotCatchCallerCancellation() = runTest {
        var caught = false
        val job = launch {
            flow<Int> { awaitCancellation() }
                .catch { caught = true }
                .collect()
        }

        runCurrent()
        job.cancelAndJoin()
        caught.shouldBeFalse()
    }
}
