package io.bluetape4k.aws.kotlin.dynamodb.examples.json

import io.bluetape4k.aws.kotlin.dynamodb.AbstractKotlinDynamoDbTest
import io.bluetape4k.aws.kotlin.dynamodb.waitForTableReady
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MovieServiceTest: AbstractKotlinDynamoDbTest() {

    companion object: KLoggingChannel() {
        private const val MOVIE_TABLE_NAME = "dynamo-movies-example"
    }

    private val movieService = MovieService(client)

    @Test
    fun `load movie data and put to dynamodb table`() = runSuspendIO {
        movieService.createMovieTable(MOVIE_TABLE_NAME)
        client.waitForTableReady(MOVIE_TABLE_NAME)

        movieService.loadMovies(MOVIE_TABLE_NAME)

        val films2222 = movieService.moviesInYear(MOVIE_TABLE_NAME, 2222)
        films2222.count shouldBeEqualTo 0

        val films2013 = movieService.moviesInYear(MOVIE_TABLE_NAME, 2013)
        films2013.count shouldBeEqualTo 2

        val titles = films2013.items!!.mapNotNull { it["title"]?.asS() }
        log.debug { "2013 film titles: " }

        titles.forEach {
            log.debug { "\ttitle=$it" }
        }
    }
}
