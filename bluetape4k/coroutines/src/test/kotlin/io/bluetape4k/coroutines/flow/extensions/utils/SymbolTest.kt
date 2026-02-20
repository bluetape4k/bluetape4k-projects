package io.bluetape4k.coroutines.flow.extensions.utils

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SymbolTest {

    @Test
    fun `Symbol은 디버그용 문자열을 제공한다`() {
        val symbol = Symbol("TEST")
        symbol.toString() shouldBeEqualTo "<TEST>"
    }

    @Test
    fun `Symbol unbox는 자기 자신을 null로 되돌린다`() {
        val symbol = Symbol("NULL")

        val value: String? = symbol.unbox(symbol)
        value shouldBeEqualTo null
    }

    @Test
    fun `Symbol unbox는 다른 값을 그대로 유지한다`() {
        val symbol = Symbol("NULL")

        val value: String? = symbol.unbox("value")
        value shouldBeEqualTo "value"
    }
}
