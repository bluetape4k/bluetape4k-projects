package io.bluetape4k.collections.immutable

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.emptyImmutableArray
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ImmutableArrayTest {

    companion object: KLogging()

    @Test
    fun `creation validation`() {
        val array = ImmutableArray(1) { "element $it" }
        array shouldBeInstanceOf ImmutableArray::class
    }

    @Test
    fun `size validation`() {
        emptyImmutableArray<String>().size shouldBeEqualTo 0
        ImmutableArray(10) { "element $it" }.size shouldBeEqualTo 10
    }
}
