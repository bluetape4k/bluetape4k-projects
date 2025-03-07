package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.AbstractExposedTest
import io.bluetape4k.exposed.domain.dto.MovieDTO
import io.bluetape4k.exposed.domain.mapper.toMovieDTO
import io.bluetape4k.exposed.domain.model.MovieSchema.withMovieAndActors
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class MovieRepositoryTest: AbstractExposedTest() {

    companion object: KLogging() {
        private fun newMovieDTO(): MovieDTO = MovieDTO(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    private val repository: MovieRepository = MovieRepository()

    @Test
    fun `find movie by id`() {
        withMovieAndActors {
            val movieId = 1L
            val movie = repository.findById(movieId).toMovieDTO()

            log.debug { "movie: $movie" }
            movie.id shouldBeEqualTo movieId
        }
    }

    @Test
    fun `search movies`() {
        withMovieAndActors {
            val params = mapOf("producerName" to "Johnny")

            val movies = repository.searchMovies(params).map { it.toMovieDTO() }
            movies.forEach {
                log.debug { "movie: $it" }
            }
            movies.size shouldBeEqualTo 2
        }
    }

    @Test
    fun `create movie`() {
        withMovieAndActors {
            val movie = newMovieDTO()

            val currentCount = repository.count()

            val savedMovie = repository.save(movie).toMovieDTO()
            savedMovie shouldBeEqualTo movie.copy(id = savedMovie.id)

            val newCount = repository.count()
            newCount shouldBeEqualTo currentCount + 1
        }
    }

    @Test
    fun `delete movie`() {
        withMovieAndActors {
            val newMovie = newMovieDTO()
            val savedMovie = repository.save(newMovie).toMovieDTO()

            val deletedCount = repository.deleteById(savedMovie.id!!)
            deletedCount shouldBeEqualTo 1
        }
    }

    @Test
    fun `get all movies and actors`() {
        withMovieAndActors {
            val movieWithActors = repository.getAllMoviesWithActors()

            movieWithActors.shouldNotBeEmpty()
            movieWithActors.forEach { movie ->
                log.debug { "movie: ${movie.name}" }
            }
        }
    }

    @Test
    fun `get movie and actors`() {
        withMovieAndActors {
            val movieId = 1L
            val movieWithActors = repository.getMovieWithActors(movieId)

            log.debug { "movieWithActors: $movieWithActors" }

            movieWithActors.shouldNotBeNull()
            movieWithActors.id shouldBeEqualTo movieId
            movieWithActors.actors shouldHaveSize 3
        }
    }

    @Test
    fun `get movie and actor count`() {
        withMovieAndActors {
            val movieWithActors = repository.getMovieActorsCount()

            movieWithActors.shouldNotBeEmpty()
            movieWithActors.forEach {
                log.debug { "movie=${it.movieName}, actor count=${it.actorCount}" }
            }
        }
    }

    @Test
    fun `find movies with acting producers`() {
        withMovieAndActors {
            val results = repository.findMoviesWithActingProducers()

            results shouldHaveSize 1
            results.forEach {
                log.debug { "movie=${it.movieName}, actor=${it.producerActorName}" }
            }
        }
    }
}
