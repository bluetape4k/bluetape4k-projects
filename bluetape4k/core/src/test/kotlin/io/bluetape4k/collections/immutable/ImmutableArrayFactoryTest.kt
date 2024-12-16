package io.bluetape4k.collections.immutable

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.ImmutableIntArray
import com.danrusu.pods4k.immutableArrays.buildImmutableArray
import com.danrusu.pods4k.immutableArrays.buildImmutableIntArray
import com.danrusu.pods4k.immutableArrays.emptyImmutableArray
import com.danrusu.pods4k.immutableArrays.emptyImmutableIntArray
import com.danrusu.pods4k.immutableArrays.immutableArrayOf
import com.danrusu.pods4k.immutableArrays.immutableArrayOfNotNull
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ImmutableArrayFactoryTest {
    companion object: KLogging()

    @Test
    fun `emptyImmutableArray validation`() {
        emptyImmutableArray<String>().size shouldBeEqualTo 0
        emptyImmutableIntArray().size shouldBeEqualTo 0
    }

    @Test
    fun `immutableArrayOf validation`() {
        val array: ImmutableArray<String> = immutableArrayOf<String>()
        array shouldBeInstanceOf ImmutableArray::class
        array.size shouldBeEqualTo 0

        val array2 = immutableArrayOf("one", "two")
        array2 shouldBeInstanceOf ImmutableArray::class
        array2.size shouldBeEqualTo 2
        array2[0] shouldBeEqualTo "one"
        array2[1] shouldBeEqualTo "two"

        val array3 = immutableArrayOf(10, 20, 30)
        array3 shouldBeInstanceOf ImmutableIntArray::class
        array3.size shouldBeEqualTo 3
    }

    @Test
    fun `immutableArrayOfNotNull validation`() {
        // Nobody would purposely attempt to pass explicit nulls to these functions since they would always be ignored.
        // Instead, these nulls come from nullable variables
        val nullString: String? = null
        val nullInt: Int? = null

        val array = immutableArrayOfNotNull<String>()
        array shouldBeEqualTo emptyImmutableArray()

        val array2 = immutableArrayOfNotNull(nullString, nullString)
        array2 shouldBeEqualTo emptyImmutableArray()

        val array3 = immutableArrayOfNotNull(nullInt)
        array3 shouldBeEqualTo emptyImmutableIntArray()

        val array4 = immutableArrayOfNotNull(nullString, "one", nullString, "two", nullString, nullString)
        array4 shouldBeEqualTo immutableArrayOf("one", "two")

        val array5 = immutableArrayOfNotNull(1, nullInt, 2)
        array5 shouldBeEqualTo immutableArrayOf(1, 2)
    }

    @Test
    fun `buildImmutableArray validation`() {
        val names = buildImmutableArray {
            add("John")
            add("Doe")
        }
        names shouldBeEqualTo immutableArrayOf("John", "Doe")

        val numbers = buildImmutableIntArray {
            add(3)
            add(7)
            add(11)
        }
        numbers shouldBeEqualTo immutableArrayOf(3, 7, 11)
    }

}
