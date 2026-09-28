package io.bluetape4k.concurrent.examples

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.concurrent.awaitTermination
import io.bluetape4k.concurrent.get
import io.bluetape4k.concurrent.getOrNull
import io.bluetape4k.concurrent.invokeAll
import io.bluetape4k.concurrent.invokeAny
import io.bluetape4k.concurrent.join
import io.bluetape4k.concurrent.joinOrNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TimeoutApiReadmeExamplesTest {

    @Test
    fun `core timeout README examples compile and run`() {
        val executor = Executors.newFixedThreadPool(2)
        try {
            val results = executor.invokeAll(listOf({ 1 }, { 2 }), 5.seconds).map { it.get() }
            results shouldBeEqualTo listOf(1, 2)
            executor.invokeAny(listOf({ 42 }), 5.seconds) shouldBeEqualTo 42

            val completed = CompletableFuture.completedFuture(42)
            val value = completed.get(5.seconds)
            val joined = completed.join(5.seconds)
            value shouldBeEqualTo 42
            joined shouldBeEqualTo 42

            val pending = CompletableFuture<Int>()
            pending.get(100.milliseconds, -1) shouldBeEqualTo -1
            pending.getOrNull(100.milliseconds) shouldBeEqualTo null
            pending.join(100.milliseconds, -1) shouldBeEqualTo -1
            pending.joinOrNull(100.milliseconds) shouldBeEqualTo null
        } finally {
            executor.shutdown()
            executor.awaitTermination(5.seconds)
        }
    }
}
