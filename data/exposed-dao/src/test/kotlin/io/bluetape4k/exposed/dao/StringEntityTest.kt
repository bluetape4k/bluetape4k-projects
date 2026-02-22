package io.bluetape4k.exposed.dao

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.entityCache
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class StringEntityTest: AbstractExposedTest() {

    object StringEntityTable: IdTable<String>("string_entity_table") {
        override val id = varchar("id", 64).entityId()
        val name = varchar("name", 100)
        override val primaryKey = PrimaryKey(id)
    }

    class StringUser(id: EntityID<String>): StringEntity(id) {
        companion object: StringEntityClass<StringUser>(StringEntityTable)

        var name by StringEntityTable.name

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StringEntity 는 문자열 ID로 저장 조회가 가능하다`(testDB: TestDB) {
        withTables(testDB, StringEntityTable) {
            val user = StringUser.new("user-001") {
                name = "Alice"
            }
            entityCache.clear()

            val loaded = StringUser.findById("user-001")!!

            loaded.id.value shouldBeEqualTo "user-001"
            loaded.idValue shouldBeEqualTo "user-001"
            loaded.name shouldBeEqualTo "Alice"
            loaded shouldBeEqualTo user
        }
    }
}
