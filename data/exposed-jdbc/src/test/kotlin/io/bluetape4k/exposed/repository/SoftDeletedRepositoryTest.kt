package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.core.HasIdentifier
import io.bluetape4k.exposed.core.dao.id.SoftDeletedIdTable
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
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
        override fun toString(): String = entityToStringBuilder()
            .add("name", name)
            .add("isDeleted", isDeleted)
            .toString()
    }

    data class ContactRecord(
        val name: String,
        val isDeleted: Boolean,
        override val id: Long = 0L,
    ): HasIdentifier<Long> {
        fun withId(id: Long) = copy(id = id)
    }

    fun ResultRow.toContactRecord(): ContactRecord = ContactRecord(
        id = this[ContactTable.id].value,
        name = this[ContactTable.name],
        isDeleted = this[ContactTable.isDeleted]
    )

    val repository = object: SoftDeletedRepository<ContactRecord, Long> {
        override val table = ContactTable
        override fun ResultRow.toEntity(): ContactRecord = ContactRecord(
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

            val activeEntities: List<ContactRecord> = repository.findActive()
            activeEntities shouldHaveSize 1
            activeEntities.single().id shouldBeEqualTo contact2Id

            repository.restoreById(contact1Id)
            val restoredEntities = repository.findActive()
            restoredEntities shouldHaveSize 2
            restoredEntities.map { it.id } shouldBeEqualTo listOf(contact1Id, contact2Id)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findActive 는 soft delete 필터와 추가 predicate 를 함께 적용한다`(testDB: TestDB) {
        withTables(testDB, ContactTable) {
            val keepId = ContactTable.insertAndGetId {
                it[name] = "Alice"
            }.value
            val deletedId = ContactTable.insertAndGetId {
                it[name] = "Alice"
            }.value
            ContactTable.insertAndGetId {
                it[name] = "Bob"
            }.value

            repository.softDeleteById(deletedId)

            val activeAlices = repository.findActive { ContactTable.name eq "Alice" }
            activeAlices shouldHaveSize 1
            activeAlices.single().id shouldBeEqualTo keepId
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countActive 와 countDeleted 는 올바른 개수를 반환한다`(testDB: TestDB) {
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
    fun `findDeleted 는 soft delete 된 엔티티만 반환한다`(testDB: TestDB) {
        withTables(testDB, ContactTable) {
            val id1 = ContactTable.insertAndGetId { it[name] = "Alice" }.value
            ContactTable.insertAndGetId { it[name] = "Bob" }

            repository.softDeleteById(id1)

            val deleted = repository.findDeleted()
            deleted shouldHaveSize 1
            deleted.single().id shouldBeEqualTo id1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `softDeleteAll 은 조건에 맞는 모든 엔티티를 soft delete 한다`(testDB: TestDB) {
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
    fun `restoreAll 은 조건에 맞는 soft delete 된 엔티티를 일괄 복원한다`(testDB: TestDB) {
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
    fun `findActivePage 는 활성 엔티티만 페이징하여 반환한다`(testDB: TestDB) {
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
