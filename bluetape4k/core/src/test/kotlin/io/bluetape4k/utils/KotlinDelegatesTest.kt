package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.lang.reflect.Constructor
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.primaryConstructor

class KotlinDelegatesTest {

    companion object: KLogging()

    @Test
    fun `immutable class 생성하기`() {

        val fooCtor: Constructor<Foo> = Foo::class.java.findPrimaryConstructor()!!
        val foo: Foo = fooCtor.instantiateClass("a", 3)!!

        foo.param1 shouldBeEqualTo "a"
        foo.param2 shouldBeEqualTo 3
    }

    @Test
    fun `named parameter를 생성자로 가지는 immutable class 생성`() {
        val barCtor: Constructor<Bar> = Bar::class.java.findPrimaryConstructor()!!
        val bar: Bar = barCtor.instantiateClass("a", 8)!!

        bar.param1 shouldBeEqualTo "a"
        bar.param2 shouldBeEqualTo 8

        val bar2 = barCtor.instantiateClass("b")

        bar2.shouldNotBeNull()
        bar2.param1 shouldBeEqualTo "b"
        bar2.param2 shouldBeEqualTo 12
    }

    @Test
    fun `Optional parameter를 생성자로 가지는 immutable class 생성`() {
        val bazCtor: Constructor<Baz> = Baz::class.java.findPrimaryConstructor()!!
        val baz: Baz? = bazCtor.instantiateClass()

        baz.shouldNotBeNull()
        baz.param1 shouldBeEqualTo "a"
        baz.param2 shouldBeEqualTo Optional.empty()
    }

    @Test
    fun `empty-arg construtor 생성자`() {
        val primaryCtor: KFunction<TwoConstructorsWithDefaultOne>? =
            TwoConstructorsWithDefaultOne::class.primaryConstructor
        primaryCtor.shouldBeNull()
    }

    @Test
    fun `인자 1개, 2개를 가진 생성자`() {
        val primaryCtor: KFunction<TwoConstructorsWithoutDefaultOne>? =
            TwoConstructorsWithoutDefaultOne::class.primaryConstructor
        primaryCtor.shouldBeNull()
    }

    @Test
    fun `빈 인자를 받는 생성자 한개만 있는 클래스`() {
        val primaryCtor: KFunction<OneConstructorWithDefaultOne>? =
            OneConstructorWithDefaultOne::class.primaryConstructor
        primaryCtor.shouldBeNull()
    }

    @Test
    fun `한개의 인자를 받는 생성자 한개가 있는 클래스`() {
        val primaryCtor: KFunction<OneConstructorWithoutDefaultOne>? =
            OneConstructorWithoutDefaultOne::class.primaryConstructor
        primaryCtor.shouldBeNull()
    }

    class Foo(val param1: String, val param2: Int)

    class Bar(val param1: String, val param2: Int = 12)

    class Baz(var param1: String = "a", var param2: Optional<Int> = Optional.empty())

    @Suppress("UNUSED_PARAMETER")
    class TwoConstructorsWithDefaultOne {
        // HINT: kotlin 의 primary constructor가 아닙니다.
        constructor()

        constructor(param1: String)
    }

    @Suppress("UNUSED_PARAMETER")
    class TwoConstructorsWithoutDefaultOne {
        // HINT: kotlin 의 primary constructor가 아닙니다.
        constructor(param1: String)

        constructor(param1: String, param2: String)
    }

    @Suppress("ConvertSecondaryConstructorToPrimary")
    class OneConstructorWithDefaultOne {
        // HINT: kotlin 의 primary constructor가 아닙니다.
        constructor()
    }

    @Suppress("UNUSED_PARAMETER", "ConvertSecondaryConstructorToPrimary")
    class OneConstructorWithoutDefaultOne {
        // HINT: kotlin 의 primary constructor가 아닙니다.
        constructor(param1: String)
    }
}
