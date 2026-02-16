package io.bluetape4k.bloomfilter


import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import net.openhft.hashing.LongHashFunction
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import java.io.Serializable

class HasherTest: AbstractBloomFilterTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `murmur3 hash offset with long type`() {
        val value = Fakers.random.nextLong(1, Long.MAX_VALUE)

        val offsets = Hasher.murmurHashOffset(value, 3, Int.MAX_VALUE)
        log.trace { "value=$value, offsets=${offsets.contentToString()}" }
        offsets shouldHaveSize 3
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `murmur3 hash offset with string type`() {
        val value = Fakers.randomString(16, 256)

        val offsets = Hasher.murmurHashOffset(value, 4, Int.MAX_VALUE)
        log.trace { "value=$value, offsets=${offsets.contentToString()}" }
        offsets shouldHaveSize 4
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `murmur3 hash offset with serializable type`() {
        val person = Person(faker.credentials().username(), faker.random().nextInt(19, 88))

        val offsets = Hasher.murmurHashOffset(person, 4, Int.MAX_VALUE)
        log.trace { "value=$person, offsets=${offsets.contentToString()}" }
        offsets shouldHaveSize 4
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `murmur3 hash offset is unique`() {

        val values = List(100) { Fakers.randomString(16, 256) }
        val offsets = values.map { Hasher.murmurHashOffset(it, 2, Int.MAX_VALUE) }

        val distincs = offsets.distinctBy { it.first() }

        distincs.size shouldBeEqualTo offsets.size
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `murmur3 hash for string is same`() {
        val murmur3 = LongHashFunction.murmur_3()

        val text = Fakers.fixedString(128)

        val hashes = List(100) { murmur3.hashChars(text) }

        hashes.distinct() shouldHaveSize 1
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `murmur3 hash for object`() {
        val murmur3 = LongHashFunction.murmur_3()

        val person1 = Person(faker.credentials().username(), faker.random().nextInt(19, 88))
        val person2 = person1.copy()
        val person3 = person1.copy(age = 9)


        val hash1 = murmur3.hashChars(person1.toString())
        val hash2 = murmur3.hashChars(person2.toString())
        val hash3 = murmur3.hashChars(person3.toString())


        log.trace { "hash1=$hash1" }
        log.trace { "hash2=$hash2" }
        log.trace { "hash3=$hash3" }

        hash2 shouldBeEqualTo hash1
        hash3 shouldNotBeEqualTo hash1
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `murmur3 hash for object as byte array`() {
        val murmur3 = LongHashFunction.murmur_3()

        val person1 = Person(faker.credentials().username(), faker.random().nextInt(19, 88))
        val person2 = person1.copy()
        val person3 = person1.copy(age = 9)


        val serializer = BinarySerializers.Jdk
        val hash1 = murmur3.hashBytes(serializer.serialize(person1))
        val hash2 = murmur3.hashBytes(serializer.serialize(person2))
        val hash3 = murmur3.hashBytes(serializer.serialize(person3))

        log.trace { "hash1=$hash1" }
        log.trace { "hash2=$hash2" }
        log.trace { "hash3=$hash3" }

        hash2 shouldBeEqualTo hash1
        hash3 shouldNotBeEqualTo hash1
    }

    data class Person(val name: String, val age: Int): Serializable
}
