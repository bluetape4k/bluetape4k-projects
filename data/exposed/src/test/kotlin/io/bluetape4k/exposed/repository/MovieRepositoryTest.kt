package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.domain.dto.MovieDTO
import io.bluetape4k.exposed.domain.mapper.toMovieDTO
import io.bluetape4k.exposed.domain.model.MovieSchema.withMovieAndActors
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MovieRepositoryTest: AbstractExposedTest() {

    companion object: KLogging() {
        private fun newMovieDTO(): MovieDTO = MovieDTO(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    private val repository: MovieRepository = MovieRepository()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find movie by id`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val movieId = 1L
            val movie = repository.findById(movieId).toMovieDTO()

            log.debug { "movie: $movie" }
            movie.id shouldBeEqualTo movieId
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `search movies`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val params = mapOf("producerName" to "Johnny")

            val movies = repository.searchMovies(params).map { it.toMovieDTO() }
            movies.forEach {
                log.debug { "movie: $it" }
            }
            movies.size shouldBeEqualTo 2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create movie`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val movie = newMovieDTO()

            val currentCount = repository.count()

            val savedMovie = repository.save(movie).toMovieDTO()
            savedMovie shouldBeEqualTo movie.copy(id = savedMovie.id)

            val newCount = repository.count()
            newCount shouldBeEqualTo currentCount + 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete movie`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val newMovie = newMovieDTO()
            val savedMovie = repository.save(newMovie).toMovieDTO()

            val deletedCount = repository.deleteById(savedMovie.id!!)
            deletedCount shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get all movies and actors`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val movieWithActors = repository.getAllMoviesWithActors()

            movieWithActors.shouldNotBeEmpty()
            movieWithActors.forEach { movie ->
                log.debug { "movie: ${movie.name}" }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get movie and actors`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val movieId = 1L
            val movieWithActors = repository.getMovieWithActors(movieId)

            log.debug { "movieWithActors: $movieWithActors" }

            movieWithActors.shouldNotBeNull()
            movieWithActors.id shouldBeEqualTo movieId
            movieWithActors.actors shouldHaveSize 3
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get movie and actor count`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val movieWithActors = repository.getMovieActorsCount()

            movieWithActors.shouldNotBeEmpty()
            movieWithActors.forEach {
                log.debug { "movie=${it.movieName}, actor count=${it.actorCount}" }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find movies with acting producers`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val results = repository.findMoviesWithActingProducers()

            results shouldHaveSize 1
            results.forEach {
                log.debug { "movie=${it.movieName}, actor=${it.producerActorName}" }
            }
        }
    }
}
