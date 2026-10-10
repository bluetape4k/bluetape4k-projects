package io.bluetape4k.spring.expression

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.Test
import org.springframework.expression.EvaluationException
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.SimpleEvaluationContext
import org.springframework.expression.spel.support.StandardEvaluationContext

class ExpressionExtensionsTest {
    data class Root(val name: String)

    private val parser = SpelExpressionParser()
    private val root = Root("value")

    @Test
    fun `evaluates all four typed overloads`() {
        parser.parseExpression("'value'").getValueAs<String>() shouldBeEqualTo "value"
        parser.parseExpression("name").getValueAs<String>(root) shouldBeEqualTo "value"
        parser.parseExpression("'value'")
            .getValueAs<String>(StandardEvaluationContext()) shouldBeEqualTo "value"
        parser.parseExpression("name")
            .getValueAs<String>(StandardEvaluationContext(), root) shouldBeEqualTo "value"
        parser.parseExpression("null").getValueAs<String>(rootObject = null).shouldBeNull()
    }

    @Test
    fun `preserves provided evaluation context policy`() {
        val context = SimpleEvaluationContext.forReadOnlyDataBinding().build()

        assertFailsWith<EvaluationException> {
            parser.parseExpression("name.substring(1)").getValueAs<String>(context, root)
        }
    }
}
