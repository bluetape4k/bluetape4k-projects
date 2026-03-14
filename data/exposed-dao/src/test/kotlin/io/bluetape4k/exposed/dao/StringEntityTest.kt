package io.bluetape4k.exposed.dao

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.entityCache
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class StringEntityTest : AbstractExposedTest() {
    object StringEntityTable : IdTable<String>("string_entity_table") {
        override val id = varchar("id", 64).entityId()
        val name = varchar("name", 100)
        override val primaryKey = PrimaryKey(id)
    }

    class StringUser(
        id: EntityID<String>,
    ) : StringEntity(id) {
        companion object : StringEntityClass<StringUser>(StringEntityTable)

        var name by StringEntityTable.name

        override fun equals(other: Any?): Boolean = idEquals(other)

        override fun hashCode(): Int = idHashCode()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StringEntity 는 문자열 ID로 저장 조회가 가능하다`(testDB: TestDB) {
        withTables(testDB, StringEntityTable) {
            val user =
                StringUser.new("user-001") {
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

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StringEntity 는 문자열 ID로 업데이트가 가능하다`(testDB: TestDB) {
        withTables(testDB, StringEntityTable) {
            StringUser.new("user-002") { name = "Bob" }
            entityCache.clear()

            val user = StringUser.findById("user-002")!!
            user.name = "Robert"
            entityCache.clear()

            StringUser.findById("user-002")!!.name shouldBeEqualTo "Robert"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StringEntity 는 문자열 ID로 삭제가 가능하다`(testDB: TestDB) {
        withTables(testDB, StringEntityTable) {
            StringUser.new("user-003") { name = "Carol" }
            entityCache.clear()

            StringUser.findById("user-003")!!.delete()
            entityCache.clear()

            StringUser.findById("user-003").shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StringEntity 는 존재하지 않는 ID 조회 시 null을 반환한다`(testDB: TestDB) {
        withTables(testDB, StringEntityTable) {
            StringUser.findById("non-existent-id").shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StringEntity 는 전체 목록 조회가 가능하다`(testDB: TestDB) {
        withTables(testDB, StringEntityTable) {
            StringUser.new("a") { name = "Alice" }
            StringUser.new("b") { name = "Bob" }
            StringUser.new("c") { name = "Carol" }
            entityCache.clear()

            StringUser.all().count() shouldBeEqualTo 3L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StringEntity 는 빈 문자열 ID로도 저장 조회가 가능하다`(testDB: TestDB) {
        withTables(testDB, StringEntityTable) {
            StringUser.new("") { name = "EmptyId" }
            entityCache.clear()

            val loaded = StringUser.findById("")!!
            loaded.name shouldBeEqualTo "EmptyId"
            loaded.idValue shouldBeEqualTo ""
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `StringEntity equals와 hashCode는 idEquals와 idHashCode를 따른다`(testDB: TestDB) {
        withTables(testDB, StringEntityTable) {
            val user1 = StringUser.new("eq-test") { name = "User1" }
            entityCache.clear()

            val user2 = StringUser.findById("eq-test")!!
            (user1 == user2).shouldBeTrue()
            user1.hashCode() shouldBeEqualTo user2.hashCode()
        }
    }
}
