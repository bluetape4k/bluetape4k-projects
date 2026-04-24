package io.bluetape4k.spring.beans

import io.bluetape4k.spring.AbstractSpringTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.BeanInstantiationException

class BeanUtilsSupportTest: AbstractSpringTest() {

    data class SampleBean(var name: String = "default", var value: Int = 0)

    open class BaseBean {
        open fun execute(): String = "base"
        fun nonExecute(): String = "non"
    }

    class DerivedBean: BaseBean() {
        override fun execute(): String = "derived"
    }

    @Test
    fun `instantiateClass - 기본 생성자로 인스턴스 생성`() {
        val instance = SampleBean::class.java.instantiateClass()
        instance.shouldNotBeNull()
        instance.name shouldBeEqualTo "default"
    }

    @Test
    fun `instantiateClass - 타입으로 인스턴스 생성`() {
        val instance = DerivedBean::class.java.instantiateClass(BaseBean::class.java)
        instance.shouldNotBeNull()
        instance.shouldBeInstanceOf<DerivedBean>()
    }

    @Test
    fun `Constructor instantiateClass - 인자로 인스턴스 생성`() {
        val ctor = SampleBean::class.java.getDeclaredConstructor(String::class.java, Int::class.javaPrimitiveType!!)
        val instance = ctor.instantiateClass("test", 42)
        instance.name shouldBeEqualTo "test"
        instance.value shouldBeEqualTo 42
    }

    @Test
    fun `findMethod - 이름으로 메서드 찾기`() {
        val method = BaseBean::class.java.findMethod("execute")
        method.shouldNotBeNull()
        method!!.name shouldBeEqualTo "execute"
    }

    @Test
    fun `findMethod - 없는 메서드는 null`() {
        val method = BaseBean::class.java.findMethod("nonExistent")
        method.shouldBeNull()
    }

    @Test
    fun `findDeclaredMethod - 선언 메서드 찾기`() {
        val method = BaseBean::class.java.findDeclaredMethod("nonExecute")
        method.shouldNotBeNull()
        method!!.name shouldBeEqualTo "nonExecute"
    }

    @Test
    fun `findMethodWithMinimalParameters - 최소 파라미터 메서드 찾기`() {
        val method = BaseBean::class.java.findMethodWithMinimalParameters("execute")
        method.shouldNotBeNull()
        method!!.name shouldBeEqualTo "execute"
    }

    @Test
    fun `findDeclaredMethodWithMinimalParameters - 최소 파라미터 선언 메서드`() {
        val method = BaseBean::class.java.findDeclaredMethodWithMinimalParameters("nonExecute")
        method.shouldNotBeNull()
    }

    @Test
    fun `Array findMethodWithMinimalParameters - 배열에서 메서드 찾기`() {
        val method = BaseBean::class.java.methods.findMethodWithMinimalParameters("execute")
        method.shouldNotBeNull()
    }

    @Test
    fun `getPropertyDescriptors - 프로퍼티 디스크립터 반환`() {
        val descriptors = SampleBean::class.java.getPropertyDescriptors()
        descriptors.shouldNotBeNull()
    }

    @Test
    fun `getPropertyDescriptor - 이름으로 프로퍼티 찾기`() {
        val descriptor = SampleBean::class.java.getPropertyDescriptor("name")
        descriptor.shouldNotBeNull()
    }

    @Test
    fun `isSimpleProperty - String은 simple property`() {
        String::class.java.isSimpleProperty().shouldBeTrue()
    }

    @Test
    fun `isSimpleValueType - Int는 simple value type`() {
        Int::class.javaObjectType.isSimpleValueType().shouldBeTrue()
    }

    @Test
    fun `copyProperties - 프로퍼티 복사`() {
        val source = SampleBean("source", 10)
        val target = SampleBean()
        source.copyProperties(target)
        target.name shouldBeEqualTo "source"
        target.value shouldBeEqualTo 10
    }

    @Test
    fun `copyProperties - ignoreProperties 제외 복사`() {
        val source = SampleBean("source", 10)
        val target = SampleBean()
        source.copyProperties(target, "name")
        target.value shouldBeEqualTo 10
    }

    @Test
    fun `copyProperties - editable 타입 범위 복사`() {
        val source = SampleBean("source", 10)
        val target = SampleBean()
        source.copyProperties(target, SampleBean::class.java)
        target.value shouldBeEqualTo 10
    }

    @Test
    fun `findMethodWithMinimalParameters - 빈 이름은 AssertionError`() {
        assertThrows<AssertionError> {
            BaseBean::class.java.findMethodWithMinimalParameters("")
        }
    }

    @Test
    fun `findMethod - 빈 이름은 AssertionError`() {
        assertThrows<AssertionError> {
            BaseBean::class.java.findMethod("")
        }
    }
}
