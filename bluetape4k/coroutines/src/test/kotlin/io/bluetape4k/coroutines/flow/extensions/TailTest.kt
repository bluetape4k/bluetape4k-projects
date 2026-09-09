package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class TailTest {

    @Test
    fun `suffix operators match collection results including nullable and oversized counts`() = runTest {
        val values = listOf(null, 1, 2, null, 3)
        for (count in listOf(0, 1, 2, 5, 8, Int.MAX_VALUE)) {
            values.asFlow().takeLast(count).toList() shouldBeEqualTo values.takeLast(count)
            values.asFlow().dropLast(count).toList() shouldBeEqualTo values.dropLast(count)
            emptyFlow<Int>().takeLast(count).toList() shouldBeEqualTo emptyList()
            emptyFlow<Int>().dropLast(count).toList() shouldBeEqualTo emptyList()
        }
    }

    @Test
    fun `negative counts fail before collection`() {
        assertFailsWith<IllegalArgumentException> { emptyFlow<Int>().takeLast(-1) }
        assertFailsWith<IllegalArgumentException> { emptyFlow<Int>().dropLast(-1) }
    }

    @Test
    fun `zero takeLast still observes upstream completion and failure`() = runTest {
        var collected = false
        flow {
            collected = true
            emit(1)
        }.takeLast(0).toList() shouldBeEqualTo emptyList()
        collected shouldBeEqualTo true
        val failure = IllegalStateException("upstream")
        assertFailsWith<IllegalStateException> {
            flow<Int> { throw failure }.takeLast(0).collect()
        } shouldBeEqualTo failure
    }

    @Test
    fun `takeLast waits for normal completion`() = runTest {
        val ready = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val values = mutableListOf<Int>()
        val job = launch {
            flow {
                emit(1)
                emit(2)
                ready.complete(Unit)
                finish.await()
            }.takeLast(1).collect { values.add(it) }
        }
        ready.await()
        values shouldBeEqualTo emptyList()
        finish.complete(Unit)
        job.join()
        values shouldBeEqualTo listOf(2)
    }

    @Test
    fun `dropLast streams with bounded lookahead and stops upstream on take`() = runTest {
        var produced = 0
        var cleaned = false
        val result = flow {
            try {
                while (true) emit(++produced)
            } finally {
                cleaned = true
            }
        }.dropLast(2).take(3).toList()
        result shouldBeEqualTo listOf(1, 2, 3)
        produced shouldBeEqualTo 5
        cleaned shouldBeEqualTo true
    }

    @Test
    fun `failures discard pending suffix without masking original error`() = runTest {
        val failure = IllegalStateException("upstream")
        val source = flow { emit(1); emit(2); emit(3); throw failure }
        val tail = mutableListOf<Int>()
        assertFailsWith<IllegalStateException> {
            source.takeLast(2).collect { tail.add(it) }
        } shouldBeEqualTo failure
        tail shouldBeEqualTo emptyList()
        val prefix = mutableListOf<Int>()
        assertFailsWith<IllegalStateException> {
            source.dropLast(2).collect { prefix.add(it) }
        } shouldBeEqualTo failure
        prefix shouldBeEqualTo listOf(1)
    }

    @Test
    fun `real cancellation cleans upstream and emits no pending suffix`() = runTest {
        val operators: List<(Flow<Int>) -> Flow<Int>> = listOf({ it.takeLast(2) }, { it.dropLast(2) })
        for (operator in operators) {
            val ready = CompletableDeferred<Unit>()
            var cleaned = false
            val values = mutableListOf<Int>()
            val job = launch {
                operator(flow {
                    try {
                        emit(1)
                        ready.complete(Unit)
                        awaitCancellation()
                    } finally {
                        cleaned = true
                    }
                }).collect { values.add(it) }
            }
            ready.await()
            job.cancelAndJoin()
            cleaned shouldBeEqualTo true
            job.isCancelled shouldBeEqualTo true
            values shouldBeEqualTo emptyList()
        }
    }

    @Test
    fun `downstream failure is transparent`() = runTest {
        val failure = IllegalArgumentException("downstream")
        for (operator in listOf<(Flow<Int>) -> Flow<Int>>({ it.takeLast(2) }, { it.dropLast(2) })) {
            var received = 0
            assertFailsWith<IllegalArgumentException> {
                operator(flowOf(1, 2, 3, 4)).collect { received++; throw failure }
            } shouldBeEqualTo failure
            received shouldBeEqualTo 1
        }
    }

    @Test
    fun `buffers are isolated between repeated and overlapping collections`() = runTest {
        var subscription = 0
        val bothStarted = CompletableDeferred<Unit>()
        val source = flow {
            val id = ++subscription
            emit(id)
            if (id == 2) bothStarted.complete(Unit)
            bothStarted.await()
            emit(id * 10)
        }
        val tail = source.takeLast(1)
        val first = async { tail.toList() }
        val second = async { tail.toList() }
        first.await() shouldBeEqualTo listOf(10)
        second.await() shouldBeEqualTo listOf(20)
        tail.toList() shouldBeEqualTo listOf(30)
        val prefix = flowOf(1, 2, 3).dropLast(1)
        repeat(2) { prefix.toList() shouldBeEqualTo listOf(1, 2) }
    }
}
