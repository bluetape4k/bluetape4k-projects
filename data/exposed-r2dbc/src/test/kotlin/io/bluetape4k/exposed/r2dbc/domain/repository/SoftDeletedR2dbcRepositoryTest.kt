package io.bluetape4k.exposed.r2dbc.domain.repository

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.core.dao.id.SoftDeletedIdTable
import io.bluetape4k.exposed.r2dbc.repository.SoftDeletedR2dbcRepository
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
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
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SoftDeletedR2dbcRepositoryTest: AbstractExposedR2dbcTest() {

    companion object: KLoggingChannel()

    object ContactTable: SoftDeletedIdTable<Long>("soft_deleted_table") {
        override val id: Column<EntityID<Long>> = long("id").autoIncrement().entityId()

        val name = varchar("name", 255)

        override val primaryKey = PrimaryKey(id)
    }

    data class ContactRecord(
        val name: String,
        val isDeleted: Boolean,
        override val id: Long = 0L,
    ): HasIdentifier<Long>

    fun ResultRow.toContactRecord(): ContactRecord = ContactRecord(
        id = this[ContactTable.id].value,
        name = this[ContactTable.name],
        isDeleted = this[ContactTable.isDeleted]
    )

    val repository = object: SoftDeletedR2dbcRepository<ContactRecord, Long> {
        override val table = ContactTable
        override suspend fun ResultRow.toEntity(): ContactRecord = toContactRecord()
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

            val activeEntities: List<ContactRecord> = repository.findActive().toList()
            activeEntities shouldHaveSize 1
            activeEntities.single().id shouldBeEqualTo contact2Id

            repository.restoreById(contact1Id)
            val restoredEntities = repository.findActive().toList()
            restoredEntities shouldHaveSize 2
            restoredEntities.map { it.id } shouldBeEqualTo listOf(contact1Id, contact2Id)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countActive 와 countDeleted 는 올바른 개수를 반환한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, ContactTable) {
            ContactTable.insertAndGetId { it[name] = "Alice" }
            ContactTable.insertAndGetId { it[name] = "Bob" }
            val id3 = ContactTable.insertAndGetId { it[name] = "Charlie" }.value

            repository.countActive() shouldBeEqualTo 3L
            repository.countDeleted() shouldBeEqualTo 0L

            repository.softDeleteById(id3)

            repository.countActive() shouldBeEqualTo 2L
            repository.countDeleted() shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findDeleted 는 soft delete 된 엔티티만 반환한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, ContactTable) {
            val id1 = ContactTable.insertAndGetId { it[name] = "Alice" }.value
            ContactTable.insertAndGetId { it[name] = "Bob" }

            repository.softDeleteById(id1)

            val deleted = repository.findDeleted().toList()
            deleted shouldHaveSize 1
            deleted.single().id shouldBeEqualTo id1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `softDeleteAll 은 조건에 맞는 모든 엔티티를 soft delete 한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, ContactTable) {
            ContactTable.insertAndGetId { it[name] = "Alice" }
            ContactTable.insertAndGetId { it[name] = "Alice" }
            ContactTable.insertAndGetId { it[name] = "Bob" }

            val affected = repository.softDeleteAll { ContactTable.name eq "Alice" }
            affected shouldBeEqualTo 2

            repository.countActive() shouldBeEqualTo 1L
            repository.countDeleted() shouldBeEqualTo 2L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `restoreAll 은 조건에 맞는 soft delete 된 엔티티를 일괄 복원한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, ContactTable) {
            ContactTable.insertAndGetId { it[name] = "Alice" }
            ContactTable.insertAndGetId { it[name] = "Alice" }
            ContactTable.insertAndGetId { it[name] = "Bob" }

            repository.softDeleteAll { ContactTable.name eq "Alice" }
            repository.countDeleted() shouldBeEqualTo 2L

            val restored = repository.restoreAll { ContactTable.name eq "Alice" }
            restored shouldBeEqualTo 2
            repository.countDeleted() shouldBeEqualTo 0L
            repository.countActive() shouldBeEqualTo 3L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findActivePage 는 활성 엔티티만 페이징하여 반환한다`(testDB: TestDB) = runSuspendIO {
        withTables(testDB, ContactTable) {
            repeat(5) { ContactTable.insertAndGetId { it[name] = faker.name().fullName() } }
            val deletedId = ContactTable.insertAndGetId { it[name] = faker.name().fullName() }.value
            repository.softDeleteById(deletedId)

            val page = repository.findActivePage(0, 3)
            page.content shouldHaveSize 3
            page.totalCount shouldBeEqualTo 5L
            page.totalPages shouldBeEqualTo 2
        }
    }
}
