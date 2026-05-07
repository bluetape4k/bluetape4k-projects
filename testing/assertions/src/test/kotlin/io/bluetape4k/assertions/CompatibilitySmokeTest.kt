package io.bluetape4k.assertions

import io.bluetape4k.assertions.coroutines.assertEmpty
import io.bluetape4k.assertions.coroutines.assertResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Instant
import io.bluetape4k.assertions.assertFailsWith

class CompatibilitySmokeTest {

    @Test
    fun `string equality works`() {
        "abc" shouldBeEqualTo "abc"
    }

    @Test
    fun `list containsAll works`() {
        listOf(1, 2, 3) shouldContainAll listOf(1, 2)
    }

    @Test
    fun `invoking shouldThrow works`() {
        assertFailsWith<IllegalStateException> { error("x") }
    }

    @Test
    fun `map shouldContainKey works`() {
        mapOf("k" to "v") shouldContainKey "k"
    }

    @Test
    fun `intArray shouldContentEqual works`() {
        intArrayOf(1, 2) shouldContentEqual intArrayOf(1, 2)
    }

    @Test
    fun `Instant shouldBeAfter works`() {
        Instant.now() shouldBeAfter Instant.now().minusSeconds(1)
    }

    @Test
    fun `assertSoftly works`() {
        assertSoftly {
            add { 1 shouldBeEqualTo 1 }
            add { "a" shouldStartWith "a" }
        }
    }

    @Test
    fun `flow assertEmpty works`() = runTest {
        flowOf<Int>().assertEmpty()
    }

    @Test
    fun `flow assertResult works`() = runTest {
        flowOf(1, 2, 3).assertResult(1, 2, 3)
    }
}
