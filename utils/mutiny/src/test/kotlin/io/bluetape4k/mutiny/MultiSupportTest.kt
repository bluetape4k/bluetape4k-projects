package io.bluetape4k.mutiny

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.coroutines.asFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.stream.IntStream
import java.util.stream.Stream

class MultiSupportTest {

    companion object: KLogging()

    @Test
    fun `create Multi instance`() {
        val m = multiOf(1, 2, 3)
            .onEach { log.debug { "item=$it" } }

        val list = m.collect().asList().await().indefinitely()
        list shouldBeEqualTo listOf(1, 2, 3)
    }

    @Test
    fun `create Multi by range`() {
        val m = multiRangeOf(10, 15)
            .onEach { log.debug { "item=$it" } }
            .collect().asList().await().indefinitely()

        m shouldBeEqualTo listOf(10, 11, 12, 13, 14)
    }

    @Test
    fun `convert Iterable to Multi`() {
        val m = listOf(1, 2, 3).asMulti()

        m.collect().asList().await().indefinitely() shouldBeEqualTo listOf(1, 2, 3)
    }

    @Test
    fun `convert Sequence to Multi`() {
        val m = listOf(1, 2, 3).asSequence().asMulti()
        m.collect().asList().await().indefinitely() shouldBeEqualTo listOf(1, 2, 3)
    }

    @Test
    fun `convert Stream to Multi`() {
        val m = IntStream.of(1, 2, 3).asMulti()
        m.collect().asList().await().indefinitely() shouldBeEqualTo listOf(1, 2, 3)

        Stream.of("a", "b", "c").asMulti()
            .collect()
            .asList()
            .await().indefinitely() shouldBeEqualTo listOf("a", "b", "c")
    }

    @Test
    fun `convert progression to Multi`() {
        val m = (1..3).asMulti()
        m.collect().asList().await().indefinitely() shouldBeEqualTo listOf(1, 2, 3)
    }

    @Test
    fun `convert Multi to Flow`() = runTest {
        Multi.createFrom()
            .ticks().every(Duration.ofMillis(10))
            .onEach {
                log.debug { "item=$it" }
            }
            .select().first(5)
            .asFlow()
            .buffer()
            .collect {
                delay(20)
                log.debug { "collect=$it" }
            }
    }
}
