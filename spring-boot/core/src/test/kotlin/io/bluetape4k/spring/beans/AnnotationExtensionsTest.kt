package io.bluetape4k.spring.beans

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.spring.AbstractSpringTest
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AliasFor
import org.springframework.util.StringValueResolver

class AnnotationExtensionsTest: AbstractSpringTest() {

    @Target(AnnotationTarget.FUNCTION)
    annotation class SampleAnnotation(
        val name: String = "default",
        val value: Int = 0,
    )

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class RouteMapping(
        val value: String = "",
    )

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    @RouteMapping
    annotation class HttpRoute(
        @get:AliasFor(annotation = RouteMapping::class, attribute = "value")
        val path: String = "",
    )

    @Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class SearchMarker(
        val value: String = "",
    )

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    @SearchMarker("meta")
    annotation class MetaSearchMarker

    class TargetBean {
        var name: String? = null
        var value: Int = -1
    }

    @SearchMarker("type")
    class MarkedClass

    @SampleAnnotation(name = "test", value = 42)
    fun annotatedMethod() {
    }

    @HttpRoute("/api/users")
    fun composedRouteMethod() {
    }

    @MetaSearchMarker
    fun metaAnnotatedMethod() {
    }

    fun plainMethod() {}

    @Test
    fun `findMergedAnnotationOrNull - composed annotation 속성을 병합한다`() {
        val method = this::class.java.getDeclaredMethod("composedRouteMethod")

        val annotation = method.findMergedAnnotationOrNull<RouteMapping>()

        annotation.shouldNotBeNull()
        annotation.value shouldBeEqualTo "/api/users"
    }

    @Test
    fun `hasMergedAnnotation - merged annotation 존재 여부를 확인한다`() {
        val annotatedMethod = this::class.java.getDeclaredMethod("composedRouteMethod")
        val plainMethod = this::class.java.getDeclaredMethod("plainMethod")

        annotatedMethod.hasMergedAnnotation<RouteMapping>().shouldBeTrue()
        plainMethod.hasMergedAnnotation<RouteMapping>().shouldBeFalse()
    }

    @Test
    fun `getMergedAnnotation - get semantics merged annotation 을 조회한다`() {
        val method = this::class.java.getDeclaredMethod("composedRouteMethod")

        val annotation = method.getMergedAnnotation<RouteMapping>()

        annotation.shouldNotBeNull()
        annotation.value shouldBeEqualTo "/api/users"
    }

    @Test
    fun `findAllMergedAnnotations - 모든 merged annotation 을 조회한다`() {
        val method = this::class.java.getDeclaredMethod("composedRouteMethod")

        val annotations = method.findAllMergedAnnotations<RouteMapping>()

        annotations.size shouldBeEqualTo 1
        annotations.single().value shouldBeEqualTo "/api/users"
    }

    @Test
    fun `findAnnotationOrNull - Method 와 Class 전용 검색 의미를 보존한다`() {
        val method = this::class.java.getDeclaredMethod("metaAnnotatedMethod")

        val methodAnnotation = method.findAnnotationOrNull<SearchMarker>()
        val classAnnotation = MarkedClass::class.java.findAnnotationOrNull<SearchMarker>()

        methodAnnotation.shouldNotBeNull()
        methodAnnotation.value shouldBeEqualTo "meta"
        classAnnotation.shouldNotBeNull()
        classAnnotation.value shouldBeEqualTo "type"
    }

    @Test
    fun `getAnnotationOrNull - present 또는 meta-present annotation 을 조회한다`() {
        val method = this::class.java.getDeclaredMethod("metaAnnotatedMethod")

        val annotation = method.getAnnotationOrNull<SearchMarker>()

        annotation.shouldNotBeNull()
        annotation.value shouldBeEqualTo "meta"
    }

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
