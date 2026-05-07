package io.bluetape4k.hibernate.querydsl.jpa

import com.querydsl.core.types.dsl.Expressions
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class ConversionsSupportTest {

    @Test
    fun `convert는 Expression을 JPA용으로 변환한다`() {
        val expr = Expressions.stringPath("name")
        val converted = expr.convert()
        converted.shouldNotBeNull()
    }

    @Test
    fun `convertForNativeQuery는 Expression을 Native Query용으로 변환한다`() {
        val expr = Expressions.stringPath("name")
        val converted = expr.convertForNativeQuery()
        converted.shouldNotBeNull()
    }
}
