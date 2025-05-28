package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.dao.id.SoftDeletedIdTable
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
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

    data class ContactDTO(
        val name: String,
        val isDeleted: Boolean,
        override val id: Long = 0L,
    ): HasIdentifier<Long>

    fun ResultRow.toContactDTO(): ContactDTO = ContactDTO(
        id = this[ContactTable.id].value,
        name = this[ContactTable.name],
        isDeleted = this[ContactTable.isDeleted]
    )

    val repository = object: SoftDeletedRepository<ContactDTO, Long> {
        override val table = ContactTable
        override fun ResultRow.toEntity(): ContactDTO = ContactDTO(
            id = this[ContactTable.id].value,
            name = this[ContactTable.name],
            isDeleted = this[ContactTable.isDeleted]
        )
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `soft deleted entity`(testDB: TestDB) {
        withTables(testDB, ContactTable) {
            val contact1Id = ContactTable.insertAndGetId {
                it[name] = faker.name().fullName()
            }.value

            val contact2Id = ContactTable.insertAndGetId {
                it[name] = faker.name().fullName()
            }.value

            repository.softDeleteById(contact1Id)

            val activeEntities: List<ContactDTO> = repository.findActive()
            activeEntities shouldHaveSize 1
            activeEntities.single().id shouldBeEqualTo contact2Id

            repository.restoreById(contact1Id)
            val restoredEntities = repository.findActive()
            restoredEntities shouldHaveSize 2
            restoredEntities.map { it.id } shouldBeEqualTo listOf(contact1Id, contact2Id)
        }
    }
}
