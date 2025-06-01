package io.bluetape4k.junit5.faker

import io.bluetape4k.junit5.model.DomainObject
import io.bluetape4k.junit5.model.NestedDomainObject
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import net.datafaker.Faker
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.Test
import java.util.*

class DataFakerExamples {

    companion object: KLogging()

    private val faker = Faker(Locale.getDefault())

    @Test
    fun `get fake names`() {
        val name = faker.name().fullName()
        val firstName = faker.name().firstName()
        val lastName = faker.name().lastName()

        val streetAddress = faker.address().streetAddress(true)

        log.trace { "name=$name" }
        log.trace { "first name=$firstName" }
        log.trace { "last name=$lastName" }
        log.trace { "street address = $streetAddress" }

        faker.zelda().game().shouldNotBeBlank()
        faker.starTrek().villain().shouldNotBeBlank()
    }

    @Test
    fun `get fake domain object`() {
        val fakeObj = fakeDomainObject()

        fakeObj.nestedDomainObject.shouldNotBeNull()
        fakeObj.nestedDomainObject!!.address!!.shouldNotBeEmpty()
        fakeObj.nestedDomainObject!!.category!!.shouldNotBeEmpty()

        fakeObj.wotsits.shouldNotBeNull()
        fakeObj.objectLists.shouldNotBeEmpty()

        log.debug { "fakeObj=$fakeObj" }
    }


    // NOTE: beanmother, random-beans, easy-random 등 라이브러리를 사용해서 fake data를 생성하는 것보다 이렇게 하는 것이 가장 좋다
    // beanmother 는 yaml 로 생성규칙을 정하므로, 속성 변경 시 refactoring에 문제가 있다.
    // random-beans 는 Faker를 이용하지 않는 순수한 Randomizer 이므로, 양의 숫자를 가져아 한다 와 같은 세심한 규칙을 줄 수 없다.
    // easy-random 은 내부적으로 Faker를 사용하지만, annotation 기반이므로, 테스트 클래스가 아닌 Production용 클래스의 경우 annotation을 지정할 수 없다.

    private fun fakeDomainObject(): DomainObject {
        return DomainObject().apply {
            id = faker.random().nextInt(1, Int.MAX_VALUE - 1)
            name = faker.name().fullName()
            value = faker.number().numberBetween(1L, Long.MAX_VALUE - 1)
            price = faker.number().randomDouble(2, 10, 1000)
            nestedDomainObject = fakeNestedDomainObject()
            wotsits = List(faker.random().nextInt(5, 10)) { faker.company().name() }

            repeat(faker.random().nextInt(2, 5)) {
                objectLists.add(fakeNestedDomainObject())
            }
        }
    }

    private fun fakeNestedDomainObject(): NestedDomainObject {
        return NestedDomainObject().apply {
            address = faker.address().fullAddress()
            category = faker.company().industry()
        }
    }

    @Test
    fun `faker numerify`() {
        val code = faker.numerify("NO-####")
        code shouldStartWith "NO-"
        log.debug { "code=$code" }
        val numbers = code.substringAfter("-")
        numbers.toLong() shouldBeGreaterThan 0L
    }

    @Test
    fun `faker letterify`() {
        val letter = faker.letterify("134-??-01-???", true)
        letter shouldStartWith "134-"
        log.debug { "letter=$letter" }
    }

    @Test
    fun `fake bothify`() {
        val fakeString = faker.bothify("??-###", true)
        fakeString shouldContain "-"
        log.debug { "fakeString=$fakeString" }
    }

    @Test
    fun `DataFaker로부터 함수문자열로 데이터 얻기`() {
        val names = faker.javaClass.methods.map { it.name }.distinct()
        log.debug { "names=${names.joinToString("\n")}" }
    }

    @Test
    fun `함수 문자열로부터 fake 값 얻기`() {
        val names = faker.getValues("name.fullName", 10)
        log.debug { "names=${names.joinToString("\n")}" }
    }

    private fun Faker.getValues(providerFullName: String, size: Int = 1): Sequence<Any> {
        val names = providerFullName.split(".", limit = 2)
        val providerName = names[0]
        val labelName = names[1]
        log.trace { "providerName=$providerName, labelName=$labelName" }

        val providerMethod = javaClass.methods.find { it.name == providerName && it.parameterCount == 0 }!!
        val provider = providerMethod.invoke(this@getValues)

        val valueMethod = provider.javaClass.methods.find { it.name == labelName && it.parameterCount == 0 }!!

        return sequence {
            repeat(size) {
                yield(valueMethod.invoke(provider))
            }
        }
    }
}
