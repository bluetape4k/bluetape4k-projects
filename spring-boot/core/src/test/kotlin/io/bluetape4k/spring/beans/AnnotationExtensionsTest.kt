package io.bluetape4k.spring.beans

import io.bluetape4k.spring.AbstractSpringTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.util.StringValueResolver

class AnnotationExtensionsTest: AbstractSpringTest() {

    @Target(AnnotationTarget.FUNCTION)
    annotation class SampleAnnotation(
        val name: String = "default",
        val value: Int = 0,
    )

    class TargetBean {
        var name: String? = null
        var value: Int = -1
    }

    @SampleAnnotation(name = "test", value = 42)
    fun annotatedMethod() {}

    @Test
    fun `copyPropertiesToBean - 애너테이션 속성을 빈에 복사`() {
        val method = this::class.java.getDeclaredMethod("annotatedMethod")
        val annotation = method.getAnnotation(SampleAnnotation::class.java)
        annotation.shouldNotBeNull()

        val bean = TargetBean()
        annotation.copyPropertiesToBean(bean)

        bean.name shouldBeEqualTo "test"
        bean.value shouldBeEqualTo 42
    }

    @Test
    fun `copyPropertiesToBean - 제외 속성은 복사하지 않음`() {
        val method = this::class.java.getDeclaredMethod("annotatedMethod")
        val annotation = method.getAnnotation(SampleAnnotation::class.java)!!

        val bean = TargetBean()
        annotation.copyPropertiesToBean(bean, "name")

        bean.name.shouldBeNull()
        bean.value shouldBeEqualTo 42
    }

    @Test
    fun `copyPropertiesToBean - StringValueResolver로 문자열 해석`() {
        val method = this::class.java.getDeclaredMethod("annotatedMethod")
        val annotation = method.getAnnotation(SampleAnnotation::class.java)!!

        val bean = TargetBean()
        val resolver = StringValueResolver { it.uppercase() }
        annotation.copyPropertiesToBean(bean, resolver)

        bean.name shouldBeEqualTo "TEST"
        bean.value shouldBeEqualTo 42
    }

    @Test
    fun `copyPropertiesToBean - valueResolver null이면 원본 값 그대로`() {
        val method = this::class.java.getDeclaredMethod("annotatedMethod")
        val annotation = method.getAnnotation(SampleAnnotation::class.java)!!

        val bean = TargetBean()
        annotation.copyPropertiesToBean(bean, null)

        bean.name shouldBeEqualTo "test"
        bean.value shouldBeEqualTo 42
    }
}
