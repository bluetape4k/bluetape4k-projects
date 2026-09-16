package io.bluetape4k.hibernate.querydsl.jpa

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class ConversionsSupportTest {

    companion object: KLogging()

    @Test
    fun `convert는 Expression을 JPA용으로 변환한다`() {
        val expr = Expressions.stringPath("name")
        val converted = expr.convert()
        converted.toString() shouldBeEqualTo "name"
    }

    @Test
    fun `convertForNativeQuery는 Expression을 Native Query용으로 변환한다`() {
        val expr = Expressions.stringPath("name")
        val converted = expr.convertForNativeQuery()
        converted.toString() shouldBeEqualTo "name"
    }
}
