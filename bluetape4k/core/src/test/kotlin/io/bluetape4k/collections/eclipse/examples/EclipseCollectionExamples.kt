package io.bluetape4k.collections.eclipse.examples

import io.bluetape4k.collections.eclipse.emptyFastList
import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.parallel.PARALLEL_EXECUTOR_SERVICE
import io.bluetape4k.collections.eclipse.primitives.toIntArrayList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.eclipse.collections.api.bag.MutableBag
import org.eclipse.collections.impl.bag.mutable.HashBag
import org.eclipse.collections.impl.list.mutable.FastList
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.concurrent.ExecutorService

class EclipseCollectionExamples {

    companion object: KLogging()

    val executor: ExecutorService = PARALLEL_EXECUTOR_SERVICE

    @Test
    fun `filter list elements`() {
        val xs = fastListOf(1, 2, 3, 4, 5)

        // select 는 filter와 같은 의미이다 (Kotlin 메소드를 사용하는 것이 더 좋다 - inline 이니까요^^)
        val even = xs.select { it % 2 == 0 }
        even shouldBeEqualTo fastListOf(2, 4)

        val odd = xs.select { it % 2 == 1 }
        odd shouldBeEqualTo fastListOf(1, 3, 5)
    }

    @ParameterizedTest
    @ValueSource(ints = [100, 1000])
    fun `execute in parallel mode`(size: Int) {
        var count = 0
        val xs = FastList.newWithNValues(size) { count++ }

        val even = xs
            .asParallel(executor, size)
            .select { it % 2 == 0 }
            .toList()

        even.size shouldBeEqualTo size / 2

        // detect is same with first
        even.detect { it % 2 == 0 } shouldBeEqualTo 0
        even.first { it % 2 == 0 } shouldBeEqualTo 0

        even.detectIndex { it % 2 == 0 } shouldBeEqualTo 0
        even.indexOfFirst { it % 2 == 0 } shouldBeEqualTo 0

        even.detectLastIndex { it % 2 == 0 } shouldBeEqualTo size / 2 - 1
        even.indexOfLast { it % 2 == 0 } shouldBeEqualTo size / 2 - 1

        even.all { it % 2 == 0 }.shouldBeTrue()

        val odd = even.detect { it % 2 == 1 }
        odd.shouldBeNull()
    }

    @ParameterizedTest
    @ValueSource(ints = [100, 1000])
    fun `partitioning list`(size: Int) {
        var count = 0
        val xs = FastList.newWithNValues(size) { count++ }

        // 참/거짓 결과로 두개로 나눈다
        val evenOdd = xs.partition { it % 2 == 0 }

        evenOdd.selected.all { it % 2 == 0 }.shouldBeTrue()
        evenOdd.rejected.all { it % 2 == 1 }.shouldBeTrue()

        evenOdd.selected.size shouldBeEqualTo size / 2
        evenOdd.rejected.size shouldBeEqualTo size / 2
    }

    @ParameterizedTest
    @ValueSource(ints = [100, 1000])
    fun `build IntArrayList`(size: Int) {
        val xs = (0 until size).toIntArrayList()

        xs.size() shouldBeEqualTo size
        xs.asLazy().select { it % 2 == 0 }.size() shouldBeEqualTo size / 2
    }

    @ParameterizedTest
    @ValueSource(ints = [100, 1000])
    fun `groupBy list`(size: Int) {
        var count = 0
        val xs = FastList.newWithNValues(size) { count++ }

        val emap = xs.groupBy { if (it % 2 == 0) "even" else "odd" }
        emap["even"].size shouldBeEqualTo size / 2
        emap["odd"].size shouldBeEqualTo size / 2
    }

    private val people = FastList.newList<Person>().apply {
        add(Person("Mary", "Smith").addPet(PetType.CAT, "Tabby", 2))
        add(Person("Bob", "Smith").addPet(PetType.CAT, "Dolly", 3).addPet(PetType.DOG, "Spot", 2))
        add(Person("Ted", "Smith").addPet(PetType.DOG, "Spike", 4))
        add(Person("Jack", "Snake").addPet(PetType.SNAKE, "Serpy", 1))
        add(Person("Barry", "Bird").addPet(PetType.BIRD, "Tweety", 2))
        add(Person("Terry", "Turtle").addPet(PetType.TURTLE, "Speedy", 1))
        add(Person("Harry", "Hamster").addPet(PetType.HAMSTER, "Fuzzy", 1).addPet(PetType.HAMSTER, "Muzzy", 1))
    }

    @Test
    fun `grouping people by lastname`() {
        val byLastname = people.groupBy { it.lastname }
        byLastname["Smith"].size shouldBeEqualTo 3
        byLastname["Smith"].map { it.firstname } shouldBeEqualTo listOf("Mary", "Bob", "Ted")
    }

    @Test
    fun `grouping people by their pets`() {
        val peopleByPets = people.groupByEach { it.petTypes }

        val catPeople = peopleByPets[PetType.CAT]
        catPeople.collect { it.firstname } shouldBeEqualTo listOf("Mary", "Bob")

        val dogPeople = peopleByPets[PetType.DOG]
        dogPeople.collect { it.firstname } shouldBeEqualTo listOf("Bob", "Ted")
    }

    @Test
    fun `get total number of pets`() {
        val numberOfPets = people.sumOfInt { it.pets.size }
        numberOfPets shouldBeEqualTo 9L
    }

    @Test
    fun `get ages of pets`() {
        val sortedAges = people.asLazy().flatCollect { it.pets }.collectInt { it.age }.toSortedList()

        sortedAges.allSatisfy { it > 0 }.shouldBeTrue()
        sortedAges.allSatisfy { it == 0 }.shouldBeFalse()
        sortedAges.allSatisfy { it < 0 }.shouldBeFalse()

        val uniqueAges = sortedAges.toSet()
        uniqueAges.toArray() shouldBeEqualTo intArrayOf(1, 2, 3, 4)
    }

    @Test
    fun `get ages of pets in kotlin`() {
        val sortedAges = people.flatMap { it.pets }.map { it.age }.sorted().toIntArray()

        sortedAges.all { it > 0 }.shouldBeTrue()
        sortedAges.all { it == 0 }.shouldBeFalse()
        sortedAges.all { it < 0 }.shouldBeFalse()

        val uniqueAges = sortedAges.toSet().toIntArray()
        uniqueAges shouldBeEqualTo intArrayOf(1, 2, 3, 4)
    }

    @Test
    fun `grouping by pet type`() {
        val counts = people.asLazy().flatCollect { it.pets }.collectInt { it.age }.toBag()

        log.debug { "counts by pet age=$counts" }

        counts.occurrencesOf(1) shouldBeEqualTo 4
        counts.occurrencesOf(2) shouldBeEqualTo 3
        counts.occurrencesOf(3) shouldBeEqualTo 1
        counts.occurrencesOf(4) shouldBeEqualTo 1
        counts.occurrencesOf(5) shouldBeEqualTo 0
        counts.occurrencesOf(0) shouldBeEqualTo 0
    }

    enum class PetType {
        CAT, DOG, HAMSTER, TURTLE, BIRD, SNAKE
    }

    data class Pet(val type: PetType, val name: String, val age: Int)

    data class Person(val firstname: String, val lastname: String, val pets: FastList<Pet> = emptyFastList()) {

        val getPetType: (Pet) -> PetType = Pet::type

        val name: String = "$firstname $lastname"

        fun named(name: String): Boolean = this.name == name

        fun hasPet(petType: PetType): Boolean =
            pets.any { it.type == petType }

        val petTypes: MutableBag<PetType>
            get() = pets.collect(Pet::type, HashBag.newBag())

        fun addPet(petType: PetType, name: String, age: Int): Person = apply {
            pets.add(Pet(petType, name, age))
        }

        val numberOfPets: Int get() = pets.size
    }
}
