package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class SimpleExpressionSupportTest {

    @Test
    fun `inValues wraps varargs`() {
        val path = Expressions.stringPath("name")

        val predicate = path.inValues("a", "b")

        predicate.toString().isNotBlank().shouldBeTrue()
    }
}
