package io.bluetape4k.junit5.random

import io.bluetape4k.junit5.model.DomainObject
import io.bluetape4k.junit5.model.getDefaultSizeOfRandom
import io.bluetape4k.junit5.model.shouldFullyPopulated
import io.bluetape4k.junit5.model.shouldPartiallyPopulated
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotBeNullOrEmpty
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.stream.Stream

@RandomizedTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD) // 필드 정보를 테스트 메소드마다 주입하기 위해
class RandomExtensionFieldTest {

    companion object: KLogging() {
        const val REPEAT_SIZE = 5
    }

    val anyStrings = ConcurrentLinkedQueue<String>()

    @RandomValue
    private lateinit var anyString: String

    @Test
    fun `inject random string`() {
        log.trace { "anyString=$anyString" }
        anyString.shouldNotBeEmpty()
    }

    @RandomValue
    private lateinit var fullyPopulatedDomainObject: DomainObject

    @Test
    fun `inject fully populated random object`() {
        fullyPopulatedDomainObject.shouldFullyPopulated()
    }

    @RandomValue(excludes = ["wotsits", "id", "nestedDomainObject.address"])
    private lateinit var partiallyPopulatedDomainObject: DomainObject

    @Test
    fun `inject partial populated random object`() {
        partiallyPopulatedDomainObject.shouldPartiallyPopulated()
    }

    @RandomValue(type = String::class)
    private lateinit var anyList: List<String>

    @Test
    fun `can inject a random list of default size`() {
        anyList.shouldNotBeNull()
        anyList.shouldNotBeEmpty()
        anyList.size shouldBeEqualTo getDefaultSizeOfRandom()
    }

    @RandomValue(size = 5, type = String::class)
    private lateinit var anyListOfSpecificSize: List<String>

    @Test
    fun `can inject a random list of specific size`() {
        anyListOfSpecificSize.shouldNotBeNull()
        anyListOfSpecificSize.shouldNotBeEmpty()
        anyListOfSpecificSize.size shouldBeEqualTo 5
    }

    @RandomValue(type = String::class)
    private lateinit var anySet: Set<String>

    @Test
    fun `can inject a random set`() {
        anySet.shouldNotBeNull()
        anySet.shouldNotBeEmpty()
        anySet.size shouldBeEqualTo getDefaultSizeOfRandom()
    }

    @RandomValue(type = String::class)
    private lateinit var anyStream: Stream<String>

    @Test
    fun `can inject a random stream`() {
        anyStream.shouldNotBeNull()
        anyStream.count() shouldBeEqualTo getDefaultSizeOfRandom().toLong()
    }

    @RandomValue(type = String::class)
    private lateinit var anyCollection: Collection<String>

    @Test
    fun `can inject a random collection`() {
        anyCollection.shouldNotBeNull()
        anyCollection.shouldNotBeEmpty()
        anyCollection.size shouldBeEqualTo getDefaultSizeOfRandom()
    }

    @RandomValue(size = 2, type = DomainObject::class)
    private lateinit var anyFullyPopulatedDomainObjects: List<DomainObject>

    @Test
    fun `can inject random fully populated domain object`() {
        anyFullyPopulatedDomainObjects.shouldNotBeNull()
        anyFullyPopulatedDomainObjects.shouldNotBeEmpty()
        anyFullyPopulatedDomainObjects.forEach {
            it.shouldFullyPopulated()
        }
    }

    @RandomValue(size = 2, type = DomainObject::class, excludes = ["wotsits", "id", "nestedDomainObject.address"])
    private lateinit var anyPartiallyPopulatedDomainObject: List<DomainObject>

    @Test
    fun `can inject random partially populated domain object`() {
        anyPartiallyPopulatedDomainObject.shouldNotBeNull()
        anyPartiallyPopulatedDomainObject.shouldNotBeEmpty()
        anyPartiallyPopulatedDomainObject.forEach {
            it.shouldPartiallyPopulated()
        }
    }

    // JUnit 5의 Parallel mode 에서는 field 값은 한번에 정해지므로, 같은 값을 가지게 된다.
    // 이럴 때를 대비해 parameter로 제공하는 것이 가장 안전한 방식이다.
    @RepeatedTest(REPEAT_SIZE)
    fun `will inject a new random value each time`() {
        log.debug { "anyString=$anyString" }
        anyString.shouldNotBeNullOrEmpty()

        if (anyStrings.isEmpty()) {
            anyStrings.add(anyString)
        } else {
            anyStrings.shouldNotContain(anyString)
            anyStrings.add(anyString)
        }
    }
}
