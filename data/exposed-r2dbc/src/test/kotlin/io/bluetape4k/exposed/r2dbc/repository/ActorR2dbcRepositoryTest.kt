package io.bluetape4k.exposed.r2dbc.repository

import io.bluetape4k.exposed.r2dbc.domain.model.ActorRecord
import io.bluetape4k.exposed.r2dbc.domain.model.MovieSchema.ActorTable
import io.bluetape4k.exposed.r2dbc.domain.model.MovieSchema.withMovieAndActors
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import kotlin.test.assertFailsWith

class ActorR2dbcRepositoryTest: AbstractExposedR2dbcTest() {

    companion object: KLoggingChannel() {
        fun newActorRecord(): ActorRecord = ActorRecord(
            id = 0L,
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    private val repository = ActorR2dbcRepository()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find actor by id`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actorId = 1L
            val actor = repository.findById(actorId)
            actor.shouldNotBeNull()
            actor.id shouldBeEqualTo actorId
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `search actors by lastName`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val params = mapOf("lastName" to "Depp")
            val actors = repository.searchActors(params).toList()

            actors.shouldNotBeEmpty()
            actors.forEach {
                log.debug { "actor: $it" }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create new actor`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actor = newActorRecord()

            val currentCount = repository.count()

            val savedActor = repository.save(actor)
            savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

            val newCount = repository.count()
            newCount shouldBeEqualTo currentCount + 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete actor by id`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actor = newActorRecord()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            val deletedCount = repository.deleteById(savedActor.id)
            deletedCount shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count of actors`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val count = repository.count()
            log.debug { "count: $count" }
            count shouldBeGreaterThan 0L

            repository.save(newActorRecord())

            val newCount = repository.count()
            newCount shouldBeEqualTo count + 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count with predicate`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val count = repository.countBy { ActorTable.lastName eq "Depp" }
            log.debug { "count: $count" }
            count shouldBeEqualTo 1L

            val op = ActorTable.lastName eq "Depp"
            val count2 = repository.countBy(op)
            log.debug { "count2: $count2" }
            count2 shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `isEmpty with Actor`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val isEmpty = repository.isEmpty()
            log.debug { "isEmpty: $isEmpty" }
            isEmpty.shouldBeFalse()

            repository.deleteAll { ActorTable.id greaterEq 0L }

            val isEmpty2 = repository.isEmpty()
            log.debug { "isEmpty2: $isEmpty2" }
            isEmpty2.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exists with Actor`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val exists = repository.exists(ActorTable.selectAll())
            log.debug { "exists: $exists" }
            exists.shouldBeTrue()

            val exists2 = repository.exists(ActorTable.selectAll().limit(1))
            log.debug { "exists2: $exists2" }
            exists2.shouldBeTrue()

            val op = ActorTable.firstName eq "Not-Exists"
            val query = ActorTable.select(ActorTable.id).where(op).limit(1)
            val exists3 = repository.exists(query)
            log.debug { "exists3: $exists3" }
            exists3.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAll with limit and offset`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            repository.findAll(limit = 2).toList() shouldHaveSize 2
            repository.findAll { ActorTable.lastName eq "Depp" }.toList() shouldHaveSize 1
            repository.findAll(limit = 3) { ActorTable.lastName eq "Depp" }.toList() shouldHaveSize 1
            repository.findAll(limit = 3, offset = 1) { ActorTable.lastName eq "Depp" }.toList() shouldHaveSize 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete entity by id`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actor = newActorRecord()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            // Delete savedActor
            repository.deleteById(savedActor.id) shouldBeEqualTo 1

            // Already deleted
            repository.deleteById(savedActor.id) shouldBeEqualTo 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete all with limit`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val count = repository.count()

            repository.deleteAll { ActorTable.lastName eq "Depp" } shouldBeEqualTo 1

            // Delete 1 actor
            repository.deleteAll() shouldBeEqualTo count.toInt() - 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete all with ignore`(testDB: TestDB) = runTest {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB }

        withMovieAndActors(testDB) {
            val count = repository.count()

            repository.deleteAllIgnore { ActorTable.lastName eq "Depp" } shouldBeEqualTo 1

            // Delete 1 actor
            repository.deleteAllIgnore() shouldBeEqualTo count.toInt() - 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findBy 는 findWithFilters 와 동일하게 동작한다`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val byLastName = repository.findBy(
                { ActorTable.lastName eq "Depp" }
            ).toList()

            val byFilter = repository.findWithFilters(
                { ActorTable.lastName eq "Depp" }
            ).toList()

            byLastName shouldBeEqualTo byFilter
            byLastName.shouldNotBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find actor by id or null`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            repository.findByIdOrNull(1L).shouldNotBeNull()
            repository.findByIdOrNull(Long.MAX_VALUE).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find with filters`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actors = repository.findWithFilters(
                { ActorTable.firstName eq "Johnny" },
                { ActorTable.lastName eq "Depp" },
            ).toList()
            actors shouldHaveSize 1
            actors.forEach {
                log.debug { "actor: $it" }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findPage 는 음수 pageNumber 를 거부한다`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            assertFailsWith<IllegalArgumentException> {
                repository.findPage(pageNumber = -1, pageSize = 10)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findPage 는 0 이하 pageSize 를 거부한다`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            assertFailsWith<IllegalArgumentException> {
                repository.findPage(pageNumber = 0, pageSize = 0)
            }
            assertFailsWith<IllegalArgumentException> {
                repository.findPage(pageNumber = 0, pageSize = -1)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find first or null`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actor = repository.findFirstOrNull { ActorTable.firstName eq "Johnny" }
            actor.shouldNotBeNull()
            actor.lastName shouldBeEqualTo "Depp"

            repository.findFirstOrNull { ActorTable.firstName eq "Not-Exists" }.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find last or null`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actor = repository.findLastOrNull { ActorTable.firstName eq "Johnny" }
            actor.shouldNotBeNull()
            actor.lastName shouldBeEqualTo "Depp"

            repository.findLastOrNull { ActorTable.firstName eq "Not-Exists" }.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find by field`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actors = repository.findByField(ActorTable.firstName, "Johnny").toList()
            actors.shouldNotBeEmpty()
            actors.single().lastName shouldBeEqualTo "Depp"

            val actors2 = repository.findByField(ActorTable.firstName, "Not-Exists").toList()
            actors2.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update by id`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actor = newActorRecord()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            val updatedCount = repository.updateById(savedActor.id) {
                it[ActorTable.firstName] = "Updated"
                it[ActorTable.lastName] = "Updated"
            }
            updatedCount shouldBeEqualTo 1

            val updatedActor = repository.findById(savedActor.id)
            updatedActor.firstName shouldBeEqualTo "Updated"
            updatedActor.lastName shouldBeEqualTo "Updated"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert with entities`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val batchCount = 10
            val entities = List(batchCount) { newActorRecord() }

            val inserted = repository.batchInsert(entities) { actor ->
                this[ActorTable.firstName] = actor.firstName
                this[ActorTable.lastName] = actor.lastName
                actor.birthday?.let { this[ActorTable.birthday] = LocalDate.parse(it) }
            }

            inserted shouldHaveSize batchCount
            inserted.all { it.id > 0L }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert with entities as sequence`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val batchCount = 10
            val entities = List(batchCount) { newActorRecord() }.asSequence()

            val inserted = repository.batchInsert(entities) { actor ->
                this[ActorTable.firstName] = actor.firstName
                this[ActorTable.lastName] = actor.lastName
                actor.birthday?.let { this[ActorTable.birthday] = LocalDate.parse(it) }
            }

            inserted shouldHaveSize batchCount
            inserted.all { it.id > 0L }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch update with entities`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val batchCount = 10
            val entities = List(batchCount) { newActorRecord() }

            val inserted = repository.batchInsert(entities) { actor ->
                this[ActorTable.firstName] = actor.firstName
                this[ActorTable.lastName] = actor.lastName
                actor.birthday?.let { this[ActorTable.birthday] = LocalDate.parse(it) }
            }

            inserted shouldHaveSize batchCount
            inserted.all { it.id > 0L }.shouldBeTrue()

            // 삽입된 actor 전체 업데이트
            val updated = repository.batchUpsert(inserted) { actor ->
                this[ActorTable.id] = actor.id
                this[ActorTable.firstName] = actor.firstName + " Updated"
                this[ActorTable.lastName] = actor.lastName + " Updated"
            }

            updated shouldHaveSize batchCount
            updated.all { it.firstName.endsWith(" Updated") }.shouldBeTrue()
            updated.all { it.lastName.endsWith(" Updated") }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch update with entities as sequence`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val batchCount = 10
            val entities = List(batchCount) { newActorRecord() }.asSequence()

            val inserted = repository.batchInsert(entities) { actor ->
                this[ActorTable.firstName] = actor.firstName
                this[ActorTable.lastName] = actor.lastName
                actor.birthday?.let { this[ActorTable.birthday] = LocalDate.parse(it) }
            }

            inserted shouldHaveSize batchCount
            inserted.all { it.id > 0L }.shouldBeTrue()

            // 삽입된 actor 전체 업데이트
            val updated = repository.batchUpsert(inserted.asSequence()) { actor ->
                this[ActorTable.id] = actor.id
                this[ActorTable.firstName] = actor.firstName + " Updated"
                this[ActorTable.lastName] = actor.lastName + " Updated"
            }

            updated shouldHaveSize batchCount
            updated.all { it.firstName.endsWith(" Updated") }.shouldBeTrue()
            updated.all { it.lastName.endsWith(" Updated") }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAllByIds 는 여러 ID 로 엔티티를 일괄 조회한다`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            // 기존 데이터에서 ID 목록 수집
            val all = repository.findAll().toList()
            val ids = all.take(2).map { it.id }

            val found = repository.findAllByIds(ids).toList()
            found shouldHaveSize 2
            found.map { it.id }.toSet() shouldBeEqualTo ids.toSet()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `existsById 는 존재하는 ID 에 true 를 반환한다`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            repository.existsById(1L).shouldBeTrue()
            repository.existsById(Long.MAX_VALUE).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `existsBy 는 조건에 맞는 레코드가 있으면 true 를 반환한다`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            repository.existsBy { ActorTable.lastName eq "Depp" }.shouldBeTrue()
            repository.existsBy { ActorTable.lastName eq "NonExistent" }.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findByFieldOrNull 은 첫 번째 매칭 엔티티를 반환하거나 null 을 반환한다`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actor = repository.findByFieldOrNull(ActorTable.lastName, "Depp")
            actor.shouldNotBeNull()
            actor.lastName shouldBeEqualTo "Depp"

            repository.findByFieldOrNull(ActorTable.lastName, "NonExistent").shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `updateAll 은 조건에 맞는 모든 엔티티를 수정한다`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actor = newActorRecord()
            val saved = repository.save(actor)

            // 특정 ID 를 제외한 모두 업데이트
            val updatedCount = repository.updateAll({ ActorTable.id eq saved.id }) {
                it[ActorTable.firstName] = "BulkUpdated"
            }
            updatedCount shouldBeEqualTo 1

            val updated = repository.findById(saved.id)
            updated.firstName shouldBeEqualTo "BulkUpdated"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `deleteAllByIds 는 여러 ID 를 일괄 삭제한다`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actors = List(3) { repository.save(newActorRecord()) }
            val ids = actors.map { it.id }

            val deleted = repository.deleteAllByIds(ids)
            deleted shouldBeEqualTo 3

            ids.forEach { id ->
                repository.findByIdOrNull(id).shouldBeNull()
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `deleteByIdIgnore 는 존재하지 않는 ID 삭제 시 예외 없이 0 을 반환한다`(testDB: TestDB) = runTest {
        // deleteIgnoreWhere 는 MySQL/MariaDB 계열에서만 지원됩니다
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB }

        withMovieAndActors(testDB) {
            // 존재하지 않는 ID 삭제 — 예외 없이 0 반환해야 함
            val deleted = repository.deleteByIdIgnore(Long.MAX_VALUE)
            deleted shouldBeEqualTo 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findPage 는 올바른 페이징 결과를 반환한다`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            // 기존 데이터 개수 확인 후 추가 삽입
            val existing = repository.count()
            repeat(5) { repository.save(newActorRecord()) }

            val total = repository.count()
            val pageSize = 3
            val page0 = repository.findPage(pageNumber = 0, pageSize = pageSize)

            page0.totalCount shouldBeEqualTo total
            page0.content shouldHaveSize pageSize
            page0.pageNumber shouldBeEqualTo 0
            page0.pageSize shouldBeEqualTo pageSize
        }
    }
}
