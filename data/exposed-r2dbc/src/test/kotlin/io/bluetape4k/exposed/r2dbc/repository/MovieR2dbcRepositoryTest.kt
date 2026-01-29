package io.bluetape4k.exposed.r2dbc.repository

import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.exposed.r2dbc.domain.MovieDTO
import io.bluetape4k.exposed.r2dbc.domain.MovieSchema.withMovieAndActors
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MovieR2dbcRepositoryTest: AbstractExposedR2dbcTest() {

    companion object: KLoggingChannel() {
        private fun newMovieDTO(): MovieDTO = MovieDTO(
            id = 0L,
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    private val repository: MovieR2dbcRepository = MovieR2dbcRepository()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find movie by id`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val movieId = 1L
            val movie = repository.findById(movieId)

            log.debug { "movie: $movie" }
            movie.id shouldBeEqualTo movieId
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `search movies`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val params = mapOf("producerName" to "Johnny")

            val movies = repository.searchMovies(params).toFastList()
            movies.forEach {
                log.debug { "movie: $it" }
            }
            movies.size shouldBeEqualTo 2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create movie`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val movie = newMovieDTO()

            val currentCount = repository.count()

            val savedMovie = repository.save(movie)
            savedMovie shouldBeEqualTo movie.copy(id = savedMovie.id)

            val newCount = repository.count()
            newCount shouldBeEqualTo currentCount + 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete movie`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val newMovie = newMovieDTO()
            val savedMovie = repository.save(newMovie)

            val deletedCount = repository.deleteById(savedMovie.id)
            deletedCount shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get all movies and actors`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val movieWithActors = repository.getAllMoviesWithActors().toFastList()

            movieWithActors.shouldNotBeEmpty()
            movieWithActors.forEach { movie ->
                log.debug { "movie: ${movie.name}" }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `get movie by id with actors`(testDB: TestDB) = runTest {
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
    fun `get movie and actor count`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val movieWithActors = repository.getMovieActorsCount().toFastList()

            movieWithActors.shouldNotBeEmpty()
            movieWithActors.forEach {
                log.debug { "movie=${it.movieName}, actor count=${it.actorCount}" }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find movies with acting producers`(testDB: TestDB) = runTest {
        withMovieAndActors(testDB) {
            val results = repository.findMoviesWithActingProducers().toFastList()

            results shouldHaveSize 1
            results.forEach {
                log.debug { "movie=${it.movieName}, actor=${it.producerActorName}" }
            }
        }
    }
}
