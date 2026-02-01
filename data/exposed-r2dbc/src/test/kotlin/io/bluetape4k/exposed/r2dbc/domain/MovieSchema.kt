package io.bluetape4k.exposed.r2dbc.domain

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.exposed.r2dbc.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import kotlinx.coroutines.flow.first
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import java.time.LocalDate


object MovieSchema: KLogging() {

    object MovieTable: LongIdTable("movies") {
        val name = varchar("name", 255)
        val producerName = varchar("producer_name", 255)
        val releaseDate = date("release_date")
    }

    object ActorTable: LongIdTable("actors") {
        val firstName = varchar("first_name", 255)
        val lastName = varchar("last_name", 255)
        val birthday = date("birthday").nullable()
    }

    object ActorInMovieTable: Table("actors_in_movies") {
        val movieId: Column<EntityID<Long>> = reference("movie_id", MovieTable, onDelete = CASCADE)
        val actorId: Column<EntityID<Long>> = reference("actor_id", ActorTable, onDelete = CASCADE)

        override val primaryKey = PrimaryKey(movieId, actorId)
    }

    @Suppress("UnusedReceiverParameter")
    suspend fun AbstractExposedR2dbcTest.withMovieAndActors(
        testDB: TestDB,
        statement: suspend R2dbcTransaction.() -> Unit,
    ) {
        withTables(testDB, MovieTable, ActorTable, ActorInMovieTable) {
            populateSampleData()
            statement()
        }
    }

    @Suppress("UnusedReceiverParameter")
    private suspend fun R2dbcTransaction.populateSampleData() {
        log.info { "Inserting sample actors and movies ..." }

        val johnnyDepp = ActorDTO(0, "Johnny", "Depp", "1979-10-28")
        val bradPitt = ActorDTO(0, "Brad", "Pitt", "1982-05-16")
        val angelinaJolie = ActorDTO(0, "Angelina", "Jolie", "1983-11-10")
        val jenniferAniston = ActorDTO(0, "Jennifer", "Aniston", "1975-07-23")
        val angelinaGrace = ActorDTO(0, "Angelina", "Grace", "1988-09-02")
        val craigDaniel = ActorDTO(0, "Craig", "Daniel", "1970-11-12")
        val ellenPaige = ActorDTO(0, "Ellen", "Paige", "1981-12-20")
        val russellCrowe = ActorDTO(0, "Russell", "Crowe", "1970-01-20")
        val edwardNorton = ActorDTO(0, "Edward", "Norton", "1975-04-03")

        val actors = listOf(
            johnnyDepp,
            bradPitt,
            angelinaJolie,
            jenniferAniston,
            angelinaGrace,
            craigDaniel,
            ellenPaige,
            russellCrowe,
            edwardNorton
        )

        val movies = listOf(
            MovieWithActorDTO(
                "Gladiator",
                johnnyDepp.firstName,
                "2000-05-01",
                fastListOf(russellCrowe, ellenPaige, craigDaniel)
            ),
            MovieWithActorDTO(
                "Guardians of the galaxy",
                johnnyDepp.firstName,
                "2014-07-21",
                fastListOf(angelinaGrace, bradPitt, ellenPaige, angelinaJolie, johnnyDepp)
            ),
            MovieWithActorDTO(
                "Fight club",
                craigDaniel.firstName,
                "1999-09-13",
                fastListOf(bradPitt, jenniferAniston, edwardNorton)
            ),
            MovieWithActorDTO(
                "13 Reasons Why",
                "Suzuki",
                "2016-01-01",
                fastListOf(angelinaJolie, jenniferAniston)
            )
        )

        ActorTable.batchInsert(actors) {
            this[ActorTable.firstName] = it.firstName
            this[ActorTable.lastName] = it.lastName
            it.birthday?.let { birthDay ->
                this[ActorTable.birthday] = LocalDate.parse(birthDay)
            }
        }

        MovieTable.batchInsert(movies) {
            this[MovieTable.name] = it.name
            this[MovieTable.producerName] = it.producerName
            this[MovieTable.releaseDate] = LocalDate.parse(it.releaseDate)
        }

        movies.forEach { movie ->
            val movieId =
                MovieTable.select(MovieTable.id).where { MovieTable.name eq movie.name }.first()[MovieTable.id]

            movie.actors.forEach { actor ->
                val actorId = ActorTable.select(ActorTable.id)
                    .where { (ActorTable.firstName eq actor.firstName) and (ActorTable.lastName eq actor.lastName) }
                    .first()[ActorTable.id]

                ActorInMovieTable.insert {
                    it[ActorInMovieTable.actorId] = actorId.value
                    it[ActorInMovieTable.movieId] = movieId.value
                }
            }
        }
    }
}
