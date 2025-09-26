package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.domain.dto.MovieActorCountDTO
import io.bluetape4k.exposed.domain.dto.MovieDTO
import io.bluetape4k.exposed.domain.dto.MovieWithActorDTO
import io.bluetape4k.exposed.domain.dto.MovieWithProducingActorDTO
import io.bluetape4k.exposed.domain.mapper.toActorDTO
import io.bluetape4k.exposed.domain.mapper.toMovieDTO
import io.bluetape4k.exposed.domain.mapper.toMovieWithActorDTO
import io.bluetape4k.exposed.domain.mapper.toMovieWithProducingActorDTO
import io.bluetape4k.exposed.domain.model.MovieSchema.ActorInMovieTable
import io.bluetape4k.exposed.domain.model.MovieSchema.ActorTable
import io.bluetape4k.exposed.domain.model.MovieSchema.MovieEntity
import io.bluetape4k.exposed.domain.model.MovieSchema.MovieTable
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate

class MovieRepository: ExposedRepository<MovieDTO, Long> {

    companion object: KLogging()

    override val table = MovieTable

    override fun ResultRow.toEntity(): MovieDTO = toMovieDTO()

    fun searchMovies(params: Map<String, String?>): List<MovieEntity> {
        log.debug { "Search movies by params. params=$params" }

        val query = table.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                MovieTable::id.name -> value?.run { query.andWhere { MovieTable.id eq value.toLong() } }

                MovieTable::name.name -> value?.run { query.andWhere { MovieTable.name eq value } }
                MovieTable::producerName.name -> value?.run {
                    query.andWhere { MovieTable.producerName eq value }
                }
                MovieTable::releaseDate.name -> value?.run {
                    query.andWhere { MovieTable.releaseDate eq LocalDate.parse(value) }
                }
            }
        }

        return MovieEntity.wrapRows(query).toList()
    }

    fun save(movieDto: MovieDTO): MovieDTO {
        log.debug { "Create new movie. movie: $movieDto" }

        val id = MovieTable.insertAndGetId {
            it[name] = movieDto.name
            it[producerName] = movieDto.producerName
            it[releaseDate] = LocalDate.parse(movieDto.releaseDate)
        }
        return movieDto.copy(id = id.value)
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
    fun getAllMoviesWithActors(): List<MovieWithActorDTO> {
        log.debug { "Get all movies with actors." }

        val join = table.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

        val movies = join
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
            .groupingBy { it[MovieTable.id] }
            .fold(mutableListOf<MovieWithActorDTO>()) { acc, row ->
                val lastMovieId = acc.lastOrNull()?.id
                if (lastMovieId != row[MovieTable.id].value) {
                    val movie = MovieWithActorDTO(
                        id = row[MovieTable.id].value,
                        name = row[MovieTable.name],
                        producerName = row[MovieTable.producerName],
                        releaseDate = row[MovieTable.releaseDate].toString(),
                    )
                    acc.add(movie)
                } else {
                    acc.lastOrNull()?.actors?.let {
                        val actor = row.toActorDTO()
                        it.add(actor)
                    }
                }
                acc
            }

        return movies.values.flatten()
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
    fun getMovieWithActors(movieId: Long): MovieWithActorDTO? {
        log.debug { "Get Movie with actors. movieId=$movieId" }

        return MovieEntity.findById(movieId)
            ?.load(MovieEntity::actors)
            ?.toMovieWithActorDTO()
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
    fun getMovieActorsCount(): List<MovieActorCountDTO> {
        log.debug { "Get Movie actors count." }

        val join = table.innerJoin(ActorInMovieTable).innerJoin(ActorTable)

        return join
            .select(MovieTable.id, MovieTable.name, ActorTable.id.count())
            .groupBy(MovieTable.id)
            .map {
                MovieActorCountDTO(
                    movieName = it[MovieTable.name],
                    actorCount = it[ActorTable.id.count()].toInt()
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
    fun findMoviesWithActingProducers(): List<MovieWithProducingActorDTO> {
        log.debug { "Find movies with acting producers." }

        val query = MovieTable
            .innerJoin(ActorInMovieTable)
            .innerJoin(ActorTable)
            .select(
                MovieTable.name,
                ActorTable.firstName,
                ActorTable.lastName
            )
            .where {
                MovieTable.producerName eq ActorTable.firstName
            }

        return query.map { it.toMovieWithProducingActorDTO() }
    }
}
