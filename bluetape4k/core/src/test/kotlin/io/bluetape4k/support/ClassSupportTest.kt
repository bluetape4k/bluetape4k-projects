package io.bluetape4k.support

import io.bluetape4k.ValueObject
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.KFunction

class ClassSupportTest {

    companion object: KLogging()

    @Test
    fun `class is present`() {
        classIsPresent("java.lang.String").shouldBeTrue()
        classIsPresent(ValueObject::class.qualifiedName!!).shouldBeTrue()
    }

    @Test
    fun `class is present in other jar`() {
        classIsPresent("org.slf4j.LoggerFactory").shouldBeTrue()
        classIsPresent(KLogging::class.qualifiedName!!).shouldBeTrue()
    }

    @Test
    fun `class is not exists`() {
        classIsPresent("xyz.NotExisting").shouldBeFalse()
    }

    @Test
    fun `존재하는 클래스를 인스턴싱합니다`() {
        RuntimeException::class.newInstanceOrNull().shouldNotBeNull()
        RuntimeException::class.java.newInstanceOrNull().shouldNotBeNull()
    }

    @Test
    fun `존재하지 않는 클래스를 생성하고자하면 null을 반환한다`() {
        classIsPresent("com.google.gson.Gson").shouldBeFalse()
        newInstanceOrNull<Any>("com.google.gson.Gson").shouldBeNull()
    }

    @Test
    fun `KClass packageName을 가져온다`() {
        String::class.packageName shouldStartWith "java.lang"
        KLogging::class.packageName shouldStartWith "io.bluetape4k.logging"
    }

    private fun describe(): String {
        return "ClassSupportTest"
    }

    @Test
    fun `KFunction qualifiedName은 클래스명과 함수명을 포함한다`() {
        val func = ClassSupportTest::class.members.first { it.name == "describe" }
        val qualifiedName = (func as KFunction<*>).qualifiedName
        log.debug { "qualifiedName: $qualifiedName" }

        qualifiedName shouldContain "ClassSupportTest.describe"
    }

    @Test
    fun `cast 성공 시 올바른 타입을 반환한다`() {
        val obj: Any = "Hello"
        val str: String = obj.cast<String>()
        str shouldBeEqualTo "Hello"
    }

    @Test
    fun `cast 실패 시 ClassCastException을 던진다`() {
        val obj: Any = "Hello"
        assertThrows<ClassCastException> {
            obj.cast<Int>()
        }
    }

    @Test
    fun `no-arg constructor가 없는 클래스는 newInstanceOrNull이 null을 반환한다`() {
        // Int는 no-arg constructor가 없음
        Int::class.newInstanceOrNull().shouldBeNull()
    }

    @Test
    fun `findAllSuperTypes는 자기 자신과 모든 상위 수형을 반환한다`() {
        val superTypes = String::class.java.findAllSuperTypes()

        superTypes.forEach {
            log.debug { "superType: $it" }
        }

        superTypes.shouldNotBeEmpty()
        superTypes shouldContain String::class.java
        superTypes shouldContain java.io.Serializable::class.java
        superTypes shouldContain Comparable::class.java
        superTypes shouldContain Any::class.java
    }

    @Test
    fun `findAllSuperTypes는 Object 클래스에 대해 자기 자신을 포함한다`() {
        val superTypes = Any::class.java.findAllSuperTypes()
        superTypes.forEach {
            log.debug { "superType: $it" }
        }
        superTypes shouldBeEqualTo listOf(Any::class.java)
    }

    @Test
    fun `getAllInterfaces는 모든 구현 인터페이스를 반환한다`() {
        val interfaces = String::class.java.getAllInterfaces()

        interfaces.forEach {
            log.debug { "interface: $it" }
        }

        interfaces.shouldNotBeEmpty()
        interfaces shouldContainAll listOf(
            java.io.Serializable::class.java,
            Comparable::class.java,
            CharSequence::class.java,
        )
    }

    @Test
    fun `getAllSuperclasses는 모든 상위 클래스를 반환한다`() {
        val superclasses = RuntimeException::class.java.getAllSuperclasses()

        superclasses.forEach {
            log.debug { "super classes: $it" }
        }

        superclasses.shouldNotBeEmpty()
        superclasses shouldContainAll listOf(
            Exception::class.java,
            Throwable::class.java,
            Any::class.java,
        )
    }

    @Test
    fun `abbrName은 축약된 클래스명을 반환한다`() {
        val abbrName = String::class.java.abbrName(13)
        log.debug { "abbrName: $abbrName" }
        abbrName.shouldNotBeNull()
        abbrName shouldContain "."
    }
}
