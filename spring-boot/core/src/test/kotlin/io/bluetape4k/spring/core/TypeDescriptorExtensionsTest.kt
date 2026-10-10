package io.bluetape4k.spring.core

import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test

class TypeDescriptorExtensionsTest {
    @Test
    fun `type descriptor contains only the raw class`() {
        typeDescriptorOf<String>().type shouldBeEqualTo String::class.java
        typeDescriptorOf<List<*>>().type shouldBeEqualTo List::class.java
    }
}
