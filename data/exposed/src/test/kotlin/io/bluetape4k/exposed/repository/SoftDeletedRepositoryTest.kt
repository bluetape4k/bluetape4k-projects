package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.dao.id.SoftDeletedIdTable
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SoftDeletedRepositoryTest: AbstractExposedTest() {

    object ContactTable: SoftDeletedIdTable<Long>("soft_deleted_table") {
        override val id: Column<EntityID<Long>> = long("id").autoIncrement().entityId()

        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    class ContactEntity(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<ContactEntity>(ContactTable)

        var name by ContactTable.name
        var isDeleted by ContactTable.isDeleted

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("isDeleted", isDeleted)
            .toString()
    }

    val repository = object: SoftDeletedRepository<ContactEntity, Long> {
        override val table = ContactTable
        override fun ResultRow.toEntity(): ContactEntity = ContactEntity.wrapRow(this)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `soft deleted entity`(testDB: TestDB) {
        withTables(testDB, ContactTable) {
            val contact1: ContactEntity = ContactEntity.new {
                name = faker.name().fullName()
            }
            val contact2: ContactEntity = ContactEntity.new {
                name = faker.name().fullName()
            }

            repository.softDeleteById(contact1.id.value)

            val activeEntities: List<ContactEntity> = repository.findActive()
            activeEntities shouldHaveSize 1
            activeEntities.single() shouldBeEqualTo contact2

            repository.restoreById(contact1.id.value)
            val restoredEntities = repository.findActive()
            restoredEntities shouldHaveSize 2
            restoredEntities shouldBeEqualTo listOf(contact1, contact2)
        }
    }
}
