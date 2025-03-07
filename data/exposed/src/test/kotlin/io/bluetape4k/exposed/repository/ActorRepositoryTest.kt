package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.domain.dto.ActorDTO
import io.bluetape4k.exposed.domain.mapper.toActorDTO
import io.bluetape4k.exposed.domain.model.MovieSchema.withMovieAndActors
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class ActorRepositoryTest: AbstractExposedTest() {

    companion object: KLogging() {
        fun newActorDTO(): ActorDTO = ActorDTO(
            firstName = faker.name().firstName(),
            lastName = faker.name().lastName(),
            birthday = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    private val repository = ActorRepository()

    @Test
    fun `find actor by id`() {
        withMovieAndActors {
            val actorId = 1L
            val actor = repository.findById(actorId).toActorDTO()
            actor.shouldNotBeNull()
            actor.id shouldBeEqualTo actorId
        }
    }

    @Test
    fun `search actors by lastName`() {
        withMovieAndActors {
            val params = mapOf("lastName" to "Depp")
            val actors = repository.searchActors(params).map { it.toActorDTO() }

            actors.shouldNotBeEmpty()
            actors.forEach {
                log.debug { "actor: $it" }
            }
        }
    }

    @Test
    fun `create new actor`() {
        withMovieAndActors {
            val actor = newActorDTO()

            val currentCount = repository.count()

            val savedActor = repository.save(actor).toActorDTO()
            savedActor shouldBeEqualTo actor.copy(id = savedActor.id)

            val newCount = repository.count()
            newCount shouldBeEqualTo currentCount + 1
        }
    }

    @Test
    fun `delete actor by id`() {
        withMovieAndActors {
            val actor = newActorDTO()
            val savedActor = repository.save(actor).toActorDTO()
            savedActor.id.shouldNotBeNull()

            val deletedCount = repository.deleteById(savedActor.id)
            deletedCount shouldBeEqualTo 1
        }
    }
}
