package io.bluetape4k.exposed.core

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.junit.jupiter.api.Test
import java.util.*

class ExposedColumnSupportsTest {

    private object TypedTable: Table("typed_table_for_supports") {
        val intCol = integer("int_col")
        val uuidCol = javaUUID("uuid_col")
    }

    @Test
    fun `convertToLanguageType 는 기본 타입 변환을 지원한다`() {
        convertToLanguageType("12", Int::class) shouldBeEqualTo 12
        convertToLanguageType("123456789", Long::class) shouldBeEqualTo 123456789L
        convertToLanguageType(123, String::class) shouldBeEqualTo "123"
    }

    @Test
    fun `convertToLanguageType 는 지원하지 않는 타입에 대해 null 을 반환한다`() {
        convertToLanguageType("true", Boolean::class).shouldBeNull()
    }

    @Test
    fun `mapToLanguageType 는 컬럼의 language type 을 사용해 변환하고 실패 항목은 제외한다`() {
        listOf("1", "2", "oops").mapToLanguageType(TypedTable.intCol) shouldBeEqualTo listOf(1, 2)

        val uuid = UUID.randomUUID()
        listOf(uuid.toString(), "not-uuid").mapToLanguageType(TypedTable.uuidCol) shouldBeEqualTo listOf(uuid)
    }
}
