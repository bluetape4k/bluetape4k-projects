package io.bluetape4k.exposed.r2dbc.domain.repository

import io.bluetape4k.exposed.r2dbc.domain.model.ActorRecord
import io.bluetape4k.exposed.r2dbc.domain.model.MovieSchema.ActorTable
import io.bluetape4k.exposed.r2dbc.domain.model.MovieSchema.withMovieAndActors
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
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
    fun `delete entity`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val actor = newActorRecord()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            // Delete savedActor
            repository.delete(savedActor) shouldBeEqualTo 1

            // Already deleted
            repository.delete(savedActor) shouldBeEqualTo 0
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
}
