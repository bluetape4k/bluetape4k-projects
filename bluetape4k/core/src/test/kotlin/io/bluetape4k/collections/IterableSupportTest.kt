package io.bluetape4k.collections

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.Test

class IterableSupportTest {

    companion object: KLogging()

    @Suppress("DIVISION_BY_ZERO")
    @Test
    fun `try mapping`() {
        val origin = List(10) { it + 1 }

        val result = origin.tryMap { it / it }
        result.all { it.isSuccess }.shouldBeTrue()

        val result2 = origin.tryMap { it / 0 }
        result2.all { it.isFailure }.shouldBeTrue()
    }

    @Suppress("DIVISION_BY_ZERO")
    @Test
    fun `mapping 시 성공한 것만 반환`() {
        val origin = List(10) { it + 1 }

        val result = origin.mapIfSuccess { it / it }
        result shouldContainAll listOf(1)

        val result2 = origin.mapIfSuccess { it / 0 }
        result2.shouldBeEmpty()
    }

    @Test
    fun `list 를 chunk 하기 - size 남기기`() {
        val list = listOf(1, 2, 3, 4, 5)
        val chunks = list.chunked(3)
        chunks shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(4, 5))
    }

    @Test
    fun `list 를 chunk 하기 - size`() {
        val list = listOf(1, 2, 3, 4, 5, 6)
        val chunks = list.chunked(3)
        chunks shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(4, 5, 6))
    }

    @Test
    fun `sliding 하기`() {
        val list = listOf(1, 2, 3, 4)

        val sliding = list.sliding(3, false)
        sliding shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(2, 3, 4))

        val sliding2 = list.sliding(3, true)
        sliding2 shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(2, 3, 4), listOf(3, 4), listOf(4))
    }

    @Test
    fun `windowing 하기`() {
        val list = listOf(1, 2, 3, 4, 5)

        val windowed = list.windowed(3, 2, false)
        windowed shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(3, 4, 5))

        val windowed2 = list.windowed(3, 2, true)
        windowed2 shouldBeEqualTo listOf(listOf(1, 2, 3), listOf(3, 4, 5), listOf(5))
    }
}
