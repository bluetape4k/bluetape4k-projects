package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class SimpleExpressionSupportTest {

    companion object: KLogging()

    @Test
    fun `inValues wraps varargs`() {
        val path = Expressions.stringPath("name")
        val predicate = path.inValues("a", "b")
        predicate.toString() shouldBeEqualTo "name in [a, b]"
    }
}
