package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.domain.dto.ActorDTO
import io.bluetape4k.exposed.domain.mapper.toActorDTO
import io.bluetape4k.exposed.domain.model.MovieSchema.withMovieAndActors
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class ActorRepositoryTest: AbstractExposedTest() {

    companion object: KLogging() {
        fun newActorDTO(): ActorDTO = ActorDTO(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    private val repository = ActorRepository()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find actor by id`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actorId = 1L
            val actor = repository.findById(actorId)
            actor.shouldNotBeNull()
            actor.id shouldBeEqualTo actorId
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `search actors by lastName`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val params = mapOf("lastName" to "Depp")
            val actors = repository.searchActors(params).map { it.toActorDTO() }

            actors.shouldNotBeEmpty()
            actors.forEach {
                log.debug { "actor: $it" }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create new actor`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = newActorDTO()

            val currentCount = repository.count()

            val savedActor = repository.save(actor)
            savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

            val newCount = repository.count()
            newCount shouldBeEqualTo currentCount + 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete actor by id`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = newActorDTO()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            val deletedCount = repository.deleteById(savedActor.id)
            deletedCount shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count of actors`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val prevCount = repository.count()
            log.debug { "count: $prevCount" }
            prevCount shouldBeGreaterThan 0L

            repository.save(newActorDTO())

            val newCount = repository.count()
            newCount shouldBeEqualTo prevCount + 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count with predicate`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val count = repository.countBy { repository.table.lastName eq "Depp" }
            log.debug { "count: $count" }
            count shouldBeEqualTo 1L

            val op = repository.table.lastName eq "Depp"
            val count2 = repository.countBy(op)
            log.debug { "count2: $count2" }
            count2 shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `isEmpty with Actor`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val isEmpty = repository.isEmpty()
            log.debug { "isEmpty: $isEmpty" }
            isEmpty.shouldBeFalse()

            repository.deleteAll { repository.table.id greaterEq 0L }

            val isEmpty2 = repository.isEmpty()
            log.debug { "isEmpty2: $isEmpty2" }
            isEmpty2.shouldBeTrue()
        }
    }

    /**
     * ```sql
     * -- H2
     * SELECT EXISTS (
     *      SELECT ACTORS.ID
     *        FROM ACTORS
     *       WHERE ACTORS.FIRST_NAME = 'Not-Exists'
     *       LIMIT 1
     *      )
     *  FROM ACTORS
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exists with Actor`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val exists = repository.exists(repository.table.selectAll())
            log.debug { "exists: $exists" }
            exists.shouldBeTrue()

            val exists2 = repository.exists(repository.table.selectAll().limit(1))
            log.debug { "exists2: $exists2" }
            exists2.shouldBeTrue()

            val op = repository.table.firstName eq "Not-Exists"
            val query = repository.table.select(repository.table.id).where(op).limit(1)
            val exists3 = repository.exists(query)
            log.debug { "exists3: $exists3" }
            exists3.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `findAll with limit and offset`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            repository.findAll(limit = 2) shouldHaveSize 2
            repository.findAll { repository.table.lastName eq "Depp" } shouldHaveSize 1
            repository.findAll(limit = 3) { repository.table.lastName eq "Depp" } shouldHaveSize 1
            repository.findAll(limit = 3, offset = 1) { repository.table.lastName eq "Depp" } shouldHaveSize 0
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete entity`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = newActorDTO()
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
    fun `delete entity by id`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = newActorDTO()
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
    fun `delete all with limit`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val count = repository.count()

            repository.deleteAll { repository.table.lastName eq "Depp" } shouldBeEqualTo 1

            // Delete 1 actor
            repository.deleteAll() shouldBeEqualTo count.toInt() - 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete all with ignore`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB }

        withMovieAndActors(testDB) {
            val count = repository.count()

            repository.deleteAllIgnore { repository.table.lastName eq "Depp" } shouldBeEqualTo 1

            // Delete 1 actor
            repository.deleteAllIgnore() shouldBeEqualTo count.toInt() - 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `countBy with predicate`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val count = repository.countBy { repository.table.lastName eq "Depp" }
            log.debug { "count: $count" }
            count shouldBeEqualTo 1L

            val op = repository.table.lastName eq "Depp"
            val count2 = repository.countBy(op)
            log.debug { "count2: $count2" }
            count2 shouldBeEqualTo 1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `exists by id`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = newActorDTO()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            // Exists
            repository.existsById(savedActor.id).shouldBeTrue()

            // Not exists
            repository.existsById(0L).shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find with filters`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actors = repository.findWithFilters(
                { repository.table.firstName eq "Johnny" },
                { repository.table.lastName eq "Depp" },
            )
            actors shouldHaveSize 1
            actors.forEach {
                log.debug { "actor: $it" }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find first or null`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = repository.findFirstOrNull { repository.table.firstName eq "Johnny" }
            actor.shouldNotBeNull()
            actor.lastName shouldBeEqualTo "Depp"

            repository.findFirstOrNull { repository.table.firstName eq "Not-Exists" }.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find last or null`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = repository.findLastOrNull { repository.table.firstName eq "Johnny" }
            actor.shouldNotBeNull()
            actor.lastName shouldBeEqualTo "Depp"

            repository.findLastOrNull { repository.table.firstName eq "Not-Exists" }.shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert with entities`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val batchCount = 10
            val entities = List(batchCount) { newActorDTO() }

            val inserted = repository.batchInsert(entities) { actor ->
                this[repository.table.firstName] = actor.firstName
                this[repository.table.lastName] = actor.lastName
                actor.birthday?.let { this[repository.table.birthday] = LocalDate.parse(it) }
            }

            inserted shouldHaveSize batchCount
            inserted.all { it.id > 0L }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert with entities as sequence`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val batchCount = 10
            val entities = List(batchCount) { newActorDTO() }.asSequence()

            val inserted = repository.batchInsert(entities) { actor ->
                this[repository.table.firstName] = actor.firstName
                this[repository.table.lastName] = actor.lastName
                actor.birthday?.let { this[repository.table.birthday] = LocalDate.parse(it) }
            }

            inserted shouldHaveSize batchCount
            inserted.all { it.id > 0L }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch update with entities`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val batchCount = 10
            val entities = List(batchCount) { newActorDTO() }

            val inserted = repository.batchInsert(entities) { actor ->
                this[repository.table.firstName] = actor.firstName
                this[repository.table.lastName] = actor.lastName
                actor.birthday?.let { this[repository.table.birthday] = LocalDate.parse(it) }
            }

            inserted shouldHaveSize batchCount
            inserted.all { it.id > 0L }.shouldBeTrue()

            // Update all inserted actors
            val updated = repository.batchUpdate(inserted) { actor ->
                this[repository.table.firstName] = actor.firstName + " Updated"
                this[repository.table.lastName] = actor.firstName + " Updated"
            }

            updated shouldHaveSize batchCount
            updated.all { it.firstName.endsWith(" Updated") }.shouldBeTrue()
            updated.all { it.lastName.endsWith(" Updated") }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch update with entities as sequence`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val batchCount = 10
            val entities = List(batchCount) { newActorDTO() }.asSequence()

            val inserted = repository.batchInsert(entities) { actor ->
                this[repository.table.firstName] = actor.firstName
                this[repository.table.lastName] = actor.lastName
                actor.birthday?.let { this[repository.table.birthday] = LocalDate.parse(it) }
            }

            inserted shouldHaveSize batchCount
            inserted.all { it.id > 0L }.shouldBeTrue()

            // Update all inserted actors
            val updated = repository.batchUpdate(inserted.asSequence()) { actor ->
                this[repository.table.firstName] = actor.firstName + " Updated"
                this[repository.table.lastName] = actor.firstName + " Updated"
            }

            updated shouldHaveSize batchCount
            updated.all { it.firstName.endsWith(" Updated") }.shouldBeTrue()
            updated.all { it.lastName.endsWith(" Updated") }.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find by field`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actors = repository.findByField(repository.table.firstName, "Johnny")
            actors.shouldNotBeEmpty()
            actors.single().lastName shouldBeEqualTo "Depp"

            val actors2 = repository.findByField(repository.table.firstName, "Not-Exists")
            actors2.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update by id`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val actor = newActorDTO()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            val updatedCount = repository.updateById(savedActor.id) {
                it[repository.table.firstName] = "Updated"
                it[repository.table.lastName] = "Updated"
            }
            updatedCount shouldBeEqualTo 1

            val updatedActor = repository.findById(savedActor.id)
            updatedActor.firstName shouldBeEqualTo "Updated"
            updatedActor.lastName shouldBeEqualTo "Updated"
        }
    }
}
