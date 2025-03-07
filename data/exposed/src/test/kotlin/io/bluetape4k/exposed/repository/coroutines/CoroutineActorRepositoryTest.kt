package io.bluetape4k.exposed.repository.coroutines

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.domain.dto.ActorDTO
import io.bluetape4k.exposed.domain.mapper.toActorDTO
import io.bluetape4k.exposed.domain.model.MovieSchema.ActorTable
import io.bluetape4k.exposed.domain.model.MovieSchema.withSuspendedMovieAndActors
import io.bluetape4k.exposed.repository.ActorRepository
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class CoroutineActorRepositoryTest: AbstractExposedTest() {

    companion object: KLogging() {
        fun newActorDTO(): ActorDTO = ActorDTO(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    private val repository = ActorRepository(ActorTable)

    @Test
    fun `find actor by id`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val actorId = 1L
            val actor = repository.findById(actorId).toActorDTO()
            actor.shouldNotBeNull()
            actor.id shouldBeEqualTo actorId
        }
    }

    @Test
    fun `search actors by lastName`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val params = mapOf("lastName" to "Depp")
            val actors = repository.searchActors(params).map { it.toActorDTO() }

            actors.shouldNotBeEmpty()
            actors.forEach {
                log.debug { "actor: $it" }
            }
        }
    }

    @Test
    fun `create new actor`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val actor = newActorDTO()

            val currentCount = repository.count()

            val savedActor = repository.save(actor).toActorDTO()
            savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

            val newCount = repository.count()
            newCount shouldBeEqualTo currentCount + 1
        }
    }

    @Test
    fun `delete actor by id`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val actor = newActorDTO()
            val savedActor = repository.save(actor).toActorDTO()
            savedActor.id.shouldNotBeNull()

            val deletedCount = repository.deleteById(savedActor.id)
            deletedCount shouldBeEqualTo 1
        }
    }

    @Test
    fun `count of actors`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val count = repository.count()
            log.debug { "count: $count" }
            count shouldBeGreaterThan 0L

            repository.save(newActorDTO())

            val newCount = repository.count()
            newCount shouldBeEqualTo count + 1L
        }
    }

    @Test
    fun `count with predicate`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val count = repository.count { ActorTable.lastName eq "Depp" }
            log.debug { "count: $count" }
            count shouldBeEqualTo 1L

            val op = ActorTable.lastName eq "Depp"
            val count2 = repository.count(op)
            log.debug { "count2: $count2" }
            count2 shouldBeEqualTo 1L
        }
    }

    @Test
    fun `isEmpty with Actor`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val isEmpty = repository.isEmpty()
            log.debug { "isEmpty: $isEmpty" }
            isEmpty.shouldBeFalse()

            repository.deleteAll { ActorTable.id greaterEq 0L }

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
    @Test
    fun `exists with Actor`() = runSuspendIO {
        withSuspendedMovieAndActors {
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

    @Test
    fun `findAll with limit and offset`() = runSuspendIO {
        withSuspendedMovieAndActors {
            repository.findAll(limit = 2) shouldHaveSize 2
            repository.findAll { ActorTable.lastName eq "Depp" } shouldHaveSize 1
            repository.findAll(limit = 3) { ActorTable.lastName eq "Depp" } shouldHaveSize 1
            repository.findAll(limit = 3, offset = 1) { ActorTable.lastName eq "Depp" } shouldHaveSize 0
        }
    }

    @Test
    fun `delete entity`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val actor = newActorDTO()
            val savedActor = repository.save(actor)
            savedActor.id.shouldNotBeNull()

            // Delete savedActor
            repository.delete(savedActor) shouldBeEqualTo 1
            // Already deleted
            repository.delete(savedActor) shouldBeEqualTo 0
        }
    }

    @Test
    fun `delete entity by id`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val actor = newActorDTO()
            val savedActor = repository.save(actor).toActorDTO()
            savedActor.id.shouldNotBeNull()

            // Delete savedActor
            repository.deleteById(savedActor.id) shouldBeEqualTo 1

            // Already deleted
            repository.deleteById(savedActor.id) shouldBeEqualTo 0
        }
    }

    @Test
    fun `delete all with limit`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val count = repository.count()

            repository.deleteAll { ActorTable.lastName eq "Depp" } shouldBeEqualTo 1

            // Delete 1 actor
            repository.deleteAll() shouldBeEqualTo count.toInt() - 1
        }
    }

    @Disabled("H2 does not support deleteIgnoreWhere")
    @Test
    fun `delete all with ignore`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val count = repository.count()

            repository.deleteAllIgnore { ActorTable.lastName eq "Depp" } shouldBeEqualTo 1

            // Delete 1 actor
            repository.deleteAllIgnore() shouldBeEqualTo count.toInt() - 1
        }
    }
}
