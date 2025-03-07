package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.domain.dto.MovieDTO
import io.bluetape4k.exposed.domain.mapper.toMovieDTO
import io.bluetape4k.exposed.domain.model.MovieSchema.withSuspendedMovieAndActors
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class MovieCoroutineRepositoryTest: AbstractExposedTest() {

    companion object: KLogging() {
        private fun newMovieDTO(): MovieDTO = MovieDTO(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    private val repository: MovieCoroutineRepository = MovieCoroutineRepository()

    @Test
    fun `find movie by id`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val movieId = 1L
            val movie = repository.findById(movieId).toMovieDTO()

            log.debug { "movie: $movie" }
            movie.id shouldBeEqualTo movieId
        }
    }

    @Test
    fun `search movies`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val params = mapOf("producerName" to "Johnny")

            val movies = repository.searchMovies(params).map { it.toMovieDTO() }
            movies.forEach {
                log.debug { "movie: $it" }
            }
            movies.size shouldBeEqualTo 2
        }
    }

    @Test
    fun `create movie`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val movie = newMovieDTO()

            val currentCount = repository.count()

            val savedMovie = repository.save(movie).toMovieDTO()
            savedMovie shouldBeEqualTo movie.copy(id = savedMovie.id)

            val newCount = repository.count()
            newCount shouldBeEqualTo currentCount + 1
        }
    }

    @Test
    fun `delete movie`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val newMovie = newMovieDTO()
            val savedMovie = repository.save(newMovie).toMovieDTO()

            val deletedCount = repository.deleteById(savedMovie.id!!)
            deletedCount shouldBeEqualTo 1
        }
    }

    @Test
    fun `get all movies and actors`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val movieWithActors = repository.getAllMoviesWithActors().toList()

            movieWithActors.shouldNotBeEmpty()
            movieWithActors.forEach { movie ->
                log.debug { "movie: ${movie.name}" }
            }
        }
    }

    @Test
    fun `get movie and actors`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val movieId = 1L
            val movieWithActors = repository.getMovieWithActors(movieId)

            log.debug { "movieWithActors: $movieWithActors" }

            movieWithActors.shouldNotBeNull()
            movieWithActors.id shouldBeEqualTo movieId
            movieWithActors.actors shouldHaveSize 3
        }
    }

    @Test
    fun `get movie and actor count`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val movieWithActors = repository.getMovieActorsCount().toList()

            movieWithActors.shouldNotBeEmpty()
            movieWithActors.forEach {
                log.debug { "movie=${it.movieName}, actor count=${it.actorCount}" }
            }
        }
    }

    @Test
    fun `find movies with acting producers`() = runSuspendIO {
        withSuspendedMovieAndActors {
            val results = repository.findMoviesWithActingProducers().toList()

            results shouldHaveSize 1
            results.forEach {
                log.debug { "movie=${it.movieName}, actor=${it.producerActorName}" }
            }
        }
    }
}
