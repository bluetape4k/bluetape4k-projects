package io.bluetape4k.spring.core

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.core.type.AnnotationMetadata

class AnnotationMetadataExtensionsTest {
    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class SampleMarker

    @SampleMarker
    class SampleConfiguration

    @Test
    fun `introspects the reified class`() {
        val actual = annotationMetadataOf<SampleConfiguration>()
        val expected = AnnotationMetadata.introspect(SampleConfiguration::class.java)

        actual.className shouldBeEqualTo SampleConfiguration::class.java.name
        actual.hasAnnotation(SampleMarker::class.java.name).shouldBeTrue()
        actual.className shouldBeEqualTo expected.className
        actual.annotationTypes shouldBeEqualTo expected.annotationTypes
    }
}
