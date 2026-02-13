package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.lang.reflect.Type
import kotlin.test.assertFailsWith

class JavaTypeSupportTest {

    private data class TypeHolder(
        val iterator: Iterator<String>,
        val list: List<String>,
    )

    @Test
    fun `actualIteratorTypeArgument는 iterator 타입 인자를 반환한다`() {
        val iteratorType: Type = TypeHolder::class.java.getDeclaredField("iterator").genericType

        iteratorType.actualIteratorTypeArgument() shouldBeEqualTo String::class.java
    }

    @Test
    fun `actualIteratorTypeArgument는 iterator가 아니면 예외를 던진다`() {
        val listType: Type = TypeHolder::class.java.getDeclaredField("list").genericType

        assertFailsWith<IllegalArgumentException> {
            listType.actualIteratorTypeArgument()
        }
    }
}

