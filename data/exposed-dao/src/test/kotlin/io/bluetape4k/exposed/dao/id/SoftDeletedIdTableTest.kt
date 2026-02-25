package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.SoftDeletedIdTable
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SoftDeletedIdTableTest: AbstractExposedTest() {

    private object SoftDeleteTable: SoftDeletedIdTable<Long>("soft_deleted_id_table") {
        override val id: Column<EntityID<Long>> = long("id").autoIncrement().entityId()
        val name = varchar("name", 100)
        override val primaryKey = PrimaryKey(id)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SoftDeletedIdTable 의 isDeleted 기본값은 false 이다`(testDB: TestDB) {
        withTables(testDB, SoftDeleteTable) {
            val id = SoftDeleteTable.insertAndGetId {
                it[name] = "Alice"
            }

            val row = SoftDeleteTable.selectAll().where { SoftDeleteTable.id eq id }.single()
            row[SoftDeleteTable.isDeleted].shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SoftDeletedIdTable 의 isDeleted 를 true 로 변경할 수 있다`(testDB: TestDB) {
        withTables(testDB, SoftDeleteTable) {
            val id = SoftDeleteTable.insertAndGetId {
                it[name] = "Bob"
            }

            SoftDeleteTable.update({ SoftDeleteTable.id eq id }) {
                it[isDeleted] = true
            }

            val row = SoftDeleteTable.selectAll().where { SoftDeleteTable.id eq id }.single()
            row[SoftDeleteTable.isDeleted].shouldBeTrue()
        }
    }
}
