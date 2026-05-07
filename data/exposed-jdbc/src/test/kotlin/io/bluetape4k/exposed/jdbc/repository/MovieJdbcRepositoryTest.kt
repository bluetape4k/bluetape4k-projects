package io.bluetape4k.exposed.jdbc.repository

import io.bluetape4k.exposed.domain.model.MovieRecord
import io.bluetape4k.exposed.domain.model.MovieSchema.withMovieAndActors
import io.bluetape4k.exposed.domain.model.toMovieRecord
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MovieJdbcRepositoryTest: AbstractExposedTest() {

    companion object: KLogging() {
        private fun newMovieRecord(): MovieRecord = MovieRecord(
            name = faker.book().title(),
            producerName = faker.name().fullName(),
            releaseDate = faker.timeAndDate().birthday(20, 80).toString()
        )
    }

    private val repository: MovieJdbcRepository = MovieJdbcRepository()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find movie by id`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val movieId = 1L
            val movie = repository.findById(movieId)

            log.debug { "movie: $movie" }
            movie.id shouldBeEqualTo movieId
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `search movies`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val params = mapOf("producerName" to "Johnny")

            val movies = repository.searchMovies(params).map { it.toMovieRecord() }
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
            val movie = newMovieRecord()

            val currentCount = repository.count()

            val savedMovie = repository.save(movie)
            savedMovie shouldBeEqualTo movie.copy(id = savedMovie.id)

            val newCount = repository.count()
            newCount shouldBeEqualTo currentCount + 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete movie`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val newMovie = newMovieRecord()
            val savedMovie = repository.save(newMovie)

            val deletedCount = repository.deleteById(savedMovie.id)
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

    /**
     * Savepoint 를 이용한 트랜잭션 롤백 시 삽입된 데이터가 취소됨을 검증합니다.
     *
     * Savepoint 설정 후 새 영화를 삽입하고 Savepoint로 롤백하면,
     * 삽입 전 개수로 되돌아가야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `savepoint 롤백 시 삽입된 데이터가 취소된다`(testDB: TestDB) {
        withMovieAndActors(testDB) {
            val countBefore = repository.count()
            log.debug { "countBefore: $countBefore" }

            // Savepoint 설정 후 영화 삽입
            val savepoint = connection.setSavepoint("before_insert")
            repository.save(newMovieRecord())
            val countAfterInsert = repository.count()
            countAfterInsert shouldBeEqualTo countBefore + 1

            // Savepoint로 롤백 — 삽입 취소
            connection.rollback(savepoint)

            // 롤백 후 개수는 삽입 전과 동일해야 함
            val countAfterRollback = repository.count()
            log.debug { "countAfterRollback: $countAfterRollback" }
            countAfterRollback shouldBeEqualTo countBefore
        }
    }
}
