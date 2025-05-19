package io.bluetape4k.collections.immutable

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.ImmutableIntArray
import com.danrusu.pods4k.immutableArrays.flatten
import com.danrusu.pods4k.immutableArrays.immutableArrayOf
import com.danrusu.pods4k.immutableArrays.toImmutableArray
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class SequencesTest {
    companion object: KLogging()

    @Test
    fun `toImmutableArray validation`() {
        // IntArray
        val array = sequenceOf(1, 3, 5).toImmutableArray()
        array shouldBeInstanceOf ImmutableIntArray::class
        array shouldBeEqualTo immutableArrayOf(1, 3, 5)

        // Array<Int>
        val array2 = sequenceOf(1, 3, 5).toImmutableArray<Int>()
        array2 shouldBeInstanceOf ImmutableArray::class
        array2 shouldBeEqualTo immutableArrayOf<Int>(1, 3, 5)
    }

    @Test
    fun `flatten validation`() {
        val strs = sequenceOf(
            immutableArrayOf("a", "bb"),
            immutableArrayOf("ccc", "dddd")
        ).flatten()
        strs shouldBeInstanceOf Sequence::class
        strs.toImmutableArray() shouldBeEqualTo immutableArrayOf("a", "bb", "ccc", "dddd")
        strs.toList() shouldBeEqualTo listOf("a", "bb", "ccc", "dddd")
    }
}
