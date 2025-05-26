package io.bluetape4k.exposed.r2dbc.repository

import io.bluetape4k.coroutines.flow.extensions.bufferUntilChanged
import io.bluetape4k.exposed.r2dbc.repository.MovieSchema.ActorInMovieTable
import io.bluetape4k.exposed.r2dbc.repository.MovieSchema.ActorTable
import io.bluetape4k.exposed.r2dbc.repository.MovieSchema.MovieTable
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.jetbrains.exposed.v1.core.Join
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import java.time.LocalDate

class MovieR2dbcRepository: ExposedR2dbcRepository<MovieDTO, Long> {

    companion object: KLogging() {
        private val MovieActorJoin: Join by lazy {
            MovieTable
                .innerJoin(ActorInMovieTable)
                .innerJoin(ActorTable)
        }

        private val moviesWithActingProducersJoin: Join by lazy {
            MovieTable
                .innerJoin(ActorInMovieTable)
                .innerJoin(
                    ActorTable,
                    onColumn = { ActorTable.id },
                    otherColumn = { ActorInMovieTable.actorId }
                ) {
                    MovieTable.producerName eq ActorTable.firstName
                }
        }
    }

    override val table = MovieTable

    override suspend fun ResultRow.toEntity(): MovieDTO = toMovieDTO()

    suspend fun save(movieDto: MovieDTO): MovieDTO {
        log.debug { "Create new movie. movie: $movieDto" }

        val id = MovieTable.insertAndGetId {
            it[name] = movieDto.name
            it[producerName] = movieDto.producerName
            it[releaseDate] = LocalDate.parse(movieDto.releaseDate)
        }
        return movieDto.copy(id = id.value)
    }

    fun searchMovies(params: Map<String, String?>): Flow<MovieDTO> {
        log.debug { "Search movies by params. params=$params" }

        val query = table.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                LongIdTable::id.name -> value?.run { query.andWhere { MovieTable.id eq value.toLong() } }

                MovieTable::name.name -> value?.run { query.andWhere { MovieTable.name eq value } }
                MovieTable::producerName.name -> value?.run {
                    query.andWhere { MovieTable.producerName eq value }
                }
                MovieTable::releaseDate.name -> value?.run {
                    query.andWhere { MovieTable.releaseDate eq LocalDate.parse(value) }
                }
            }
        }

        return query.map { it.toEntity() }
    }

    /**
     * ```sql
     * -- H2
     * SELECT MOVIES.ID,
     *        MOVIES."name",
     *        MOVIES.PRODUCER_NAME,
     *        MOVIES.RELEASE_DATE,
     *        ACTORS.ID,
     *        ACTORS.FIRST_NAME,
     *        ACTORS.LAST_NAME,
     *        ACTORS.BIRTHDAY
     *   FROM MOVIES
     *          INNER JOIN ACTORS_IN_MOVIES ON MOVIES.ID = ACTORS_IN_MOVIES.MOVIE_ID
     *          INNER JOIN ACTORS ON ACTORS.ID = ACTORS_IN_MOVIES.ACTOR_ID
     */
    fun getAllMoviesWithActors(): Flow<MovieWithActorDTO> {
        log.debug { "Get all movies with actors." }

        return MovieActorJoin
            .select(
                MovieTable.id,
                MovieTable.name,
                MovieTable.producerName,
                MovieTable.releaseDate,
                ActorTable.id,
                ActorTable.firstName,
                ActorTable.lastName,
                ActorTable.birthday
            )
            .map { row ->
                val movie = row.toMovieDTO()
                val actor = row.toActorDTO()

                movie to actor
            }
            .bufferUntilChanged { it.first.id }
            .mapNotNull { pairs ->
                val movie = pairs.first().first
                val actors = pairs.map { it.second }
                movie.toMovieWithActorDTO(actors)
            }
    }

    /**
     * `movieId` 에 대한 영화와 그 영화에 출연한 배우를 조회합니다.
     *
     * ```sql
     * -- H2
     * SELECT MOVIES.ID, MOVIES."name", MOVIES.PRODUCER_NAME, MOVIES.RELEASE_DATE
     *   FROM MOVIES
     *  WHERE MOVIES.ID = 1;
     *
     * SELECT ACTORS.ID,
     *        ACTORS.FIRST_NAME,
     *        ACTORS.LAST_NAME,
     *        ACTORS.BIRTHDAY,
     *        ACTORS_IN_MOVIES.MOVIE_ID,
     *        ACTORS_IN_MOVIES.ACTOR_ID
     *   FROM ACTORS INNER JOIN ACTORS_IN_MOVIES ON ACTORS_IN_MOVIES.ACTOR_ID = ACTORS.ID
     *  WHERE ACTORS_IN_MOVIES.MOVIE_ID = 1;
     * ```
     */
    suspend fun getMovieWithActors(movieId: Long): MovieWithActorDTO? {
        log.debug { "Get movie with actors. movieId: $movieId" }

        return MovieActorJoin
            .select(
                MovieTable.id,
                MovieTable.name,
                MovieTable.producerName,
                MovieTable.releaseDate,
                ActorTable.id,
                ActorTable.firstName,
                ActorTable.lastName,
                ActorTable.birthday
            )
            .where { MovieTable.id eq movieId }
            .map { row ->
                val movie = row.toMovieDTO()
                val actor = row.toActorDTO()

                movie to actor
            }
            .bufferUntilChanged { it.first.id }
            .mapNotNull { pairs ->
                val movie = pairs.first().first
                val actors = pairs.map { it.second }
                movie.toMovieWithActorDTO(actors)
            }
            .firstOrNull()
    }


    /**
     * ```sql
     * -- H2
     * SELECT MOVIES.ID,
     *        MOVIES."name",
     *        COUNT(ACTORS.ID)
     *   FROM MOVIES
     *          INNER JOIN ACTORS_IN_MOVIES ON MOVIES.ID = ACTORS_IN_MOVIES.MOVIE_ID
     *          INNER JOIN ACTORS ON ACTORS.ID = ACTORS_IN_MOVIES.ACTOR_ID
     *  GROUP BY MOVIES.ID
     * ```
     */
    fun getMovieActorsCount(): Flow<MovieActorCountDTO> {
        log.debug { "Get Movie actors count." }

        val actorCountAlias = ActorTable.id.count().alias("actorCount")

        return MovieActorJoin
            .select(MovieTable.id, MovieTable.name, actorCountAlias)
            .groupBy(MovieTable.id)
            .map {
                MovieActorCountDTO(
                    movieName = it[MovieTable.name],
                    actorCount = it[actorCountAlias].toInt()
                )
            }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT movies."name",
     *       actors.first_name,
     *       actors.last_name
     *  FROM movies
     *      INNER JOIN actors_in_movies ON movies.id = actors_in_movies.movie_id
     *      INNER JOIN actors ON actors.id = actors_in_movies.actor_id
     *  WHERE movies.producer_name = actors.first_name
     * ```
     */
    fun findMoviesWithActingProducers(): Flow<MovieWithProducingActorDTO> {
        log.debug { "Find movies with acting producers." }

        return moviesWithActingProducersJoin
            .select(
                MovieTable.name,
                ActorTable.firstName,
                ActorTable.lastName
            )
            .map {
                it.toMovieWithProducingActorDTO()
            }
    }
}
