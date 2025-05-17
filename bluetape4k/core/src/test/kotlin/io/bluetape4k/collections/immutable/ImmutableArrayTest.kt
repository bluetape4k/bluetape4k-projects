package io.bluetape4k.collections.immutable

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.emptyImmutableArray
import com.danrusu.pods4k.immutableArrays.immutableArrayOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ImmutableArrayTest {

    companion object: KLogging()

    @Test
    fun `immutable arrays are covariant`() {
        open class Person(val name: String)
        class Manager(name: String, numEmployees: Int): Person(name)

        val bob = Manager("Bob", 3)
        val managers: ImmutableArray<Manager> = immutableArrayOf(bob)

        // This should be allowed because every manager is also a person
        val people: ImmutableArray<Person> = managers

        // The managers ImmutableArray<Manager> is safe from heap pollution when referenced as an
        // ImmutableArray<Person> because it's immutable, so we can't add a regular non-manager person into it
        people.single() shouldBeEqualTo bob
    }

    @Test
    fun `creation validation`() {
        val array = ImmutableArray(1) { "element $it" }
        array shouldBeInstanceOf ImmutableArray::class

        // Cannot create with a negative size
        assertFailsWith<NegativeArraySizeException> {
            ImmutableArray(-1) { "element $it" }
        }
    }

    @Test
    fun `size validation`() {
        emptyImmutableArray<String>().size shouldBeEqualTo 0
        ImmutableArray(10) { "element $it" }.size shouldBeEqualTo 10
    }

    @Test
    fun `lastIndex validation`() {
        emptyImmutableArray<String>().lastIndex shouldBeEqualTo -1
        immutableArrayOf("one").lastIndex shouldBeEqualTo 0
        ImmutableArray(10) { "element $it" }.lastIndex shouldBeEqualTo 9
    }

    @Test
    fun `indices validation`() {
        immutableArrayOf("one", "two", "three").indices shouldBeEqualTo 0..2
    }
}
