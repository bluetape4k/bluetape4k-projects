package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.domain.mapper.toActorDTO
import io.bluetape4k.exposed.domain.model.MovieSchema.withSuspendedMovieAndActors
import io.bluetape4k.exposed.repository.ActorRepositoryTest.Companion.newActorDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class ActorCoroutineRepositoryTest: AbstractExposedTest() {

    companion object: KLogging()

    private val repository = ActorCoroutineRepository()

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
            val actors = repository.searchActors(params).map { it.toActorDTO() }.toList()

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
}
