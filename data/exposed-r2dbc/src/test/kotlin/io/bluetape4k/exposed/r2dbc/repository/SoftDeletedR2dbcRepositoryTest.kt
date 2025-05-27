package io.bluetape4k.exposed.r2dbc.repository

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.dao.id.SoftDeletedIdTable
import io.bluetape4k.exposed.r2dbc.tests.R2dbcExposedTestBase
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SoftDeletedR2dbcRepositoryTest: R2dbcExposedTestBase() {

    companion object: KLoggingChannel()

    object ContactTable: SoftDeletedIdTable<Long>("soft_deleted_table") {
        override val id: Column<EntityID<Long>> = long("id").autoIncrement().entityId()

        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
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

    val repository = object: SoftDeletedR2dbcRepository<ContactDTO, Long> {
        override val table = ContactTable
        override suspend fun ResultRow.toEntity(): ContactDTO = ContactDTO(
            id = this[ContactTable.id].value,
            name = this[ContactTable.name],
            isDeleted = this[ContactTable.isDeleted]
        )
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `soft deleted entity`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, ContactTable) {
            val contact1Id = ContactTable.insertAndGetId {
                it[name] = faker.name().fullName()
            }.value

            val contact2Id = ContactTable.insertAndGetId {
                it[name] = faker.name().fullName()
            }.value

            repository.softDeleteById(contact1Id)

            val activeEntities: List<ContactDTO> = repository.findActive().toList()
            activeEntities shouldHaveSize 1
            activeEntities.single().id shouldBeEqualTo contact2Id

            repository.restoreById(contact1Id)
            val restoredEntities = repository.findActive().toList()
            restoredEntities shouldHaveSize 2
            restoredEntities.map { it.id } shouldBeEqualTo listOf(contact1Id, contact2Id)
        }
    }
}
