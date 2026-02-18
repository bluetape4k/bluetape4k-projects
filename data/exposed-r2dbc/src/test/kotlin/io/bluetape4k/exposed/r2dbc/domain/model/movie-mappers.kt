package io.bluetape4k.exposed.r2dbc.domain.model

import io.bluetape4k.exposed.r2dbc.domain.model.MovieSchema.ActorTable
import io.bluetape4k.exposed.r2dbc.domain.model.MovieSchema.MovieTable
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toActorRecord() = ActorRecord(
    id = this[ActorTable.id].value,
    firstName = this[ActorTable.firstName],
    lastName = this[ActorTable.lastName],
    birthday = this[ActorTable.birthday]?.toString()
)

fun ResultRow.toMovieRecord() = MovieRecord(
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    id = this[MovieTable.id].value
)

fun ResultRow.toMovieWithActorRecord(actors: List<ActorRecord>) =
    MovieWithActorRecord(
        name = this[MovieTable.name],
        producerName = this[MovieTable.producerName],
        releaseDate = this[MovieTable.releaseDate].toString(),
        actors = actors.toList(),
        id = this[MovieTable.id].value
    )

fun MovieRecord.toMovieWithActorRecord(actors: Collection<ActorRecord>) =
    MovieWithActorRecord(
        name = this.name,
        producerName = this.producerName,
        releaseDate = this.releaseDate,
        actors = actors.toList(),
        id = this.id
    )

fun ResultRow.toMovieWithProducingActorRecord() = MovieWithProducingActorRecord(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
