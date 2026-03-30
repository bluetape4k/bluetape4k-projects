package io.bluetape4k.exposed.r2dbc.redisson.map

import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.redisson.api.AsyncIterator
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class AsyncIteratorSupportTest {

    @Test
    fun `toList 는 AsyncIterator 의 모든 값을 순서대로 수집한다`() = runSuspendIO {
        val iterator = object : AsyncIterator<Int> {
            private val values = listOf(1, 2, 3).iterator()
            private var nextValue: Int? = null

            override fun hasNext(): CompletionStage<Boolean?> {
                if (nextValue != null) return CompletableFuture.completedFuture(true)

                return if (values.hasNext()) {
                    nextValue = values.next()
                    CompletableFuture.completedFuture(true)
                } else {
                    CompletableFuture.completedFuture(false)
                }
            }

            override fun next(): CompletionStage<Int> {
                val value = nextValue ?: throw NoSuchElementException("No more elements")
                nextValue = null
                return CompletableFuture.completedFuture(value)
            }
        }

        iterator.toList() shouldBeEqualTo listOf(1, 2, 3)
    }
}
