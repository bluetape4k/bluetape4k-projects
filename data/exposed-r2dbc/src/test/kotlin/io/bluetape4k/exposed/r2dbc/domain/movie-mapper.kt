package io.bluetape4k.exposed.r2dbc.domain

import io.bluetape4k.exposed.r2dbc.domain.MovieSchema.ActorTable
import io.bluetape4k.exposed.r2dbc.domain.MovieSchema.MovieTable
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toActorDTO() = ActorDTO(
    id = this[ActorTable.id].value,
    firstName = this[ActorTable.firstName],
    lastName = this[ActorTable.lastName],
    birthday = this[ActorTable.birthday]?.toString()
)

fun ResultRow.toMovieDTO() = MovieDTO(
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    id = this[MovieTable.id].value
)

fun ResultRow.toMovieWithActorDTO(actors: List<ActorDTO>) =
    MovieWithActorDTO(
        name = this[MovieTable.name],
        producerName = this[MovieTable.producerName],
        releaseDate = this[MovieTable.releaseDate].toString(),
        actors = actors.toMutableList(),
        id = this[MovieTable.id].value
    )

fun MovieDTO.toMovieWithActorDTO(actors: Collection<ActorDTO>) =
    MovieWithActorDTO(
        name = this.name,
        producerName = this.producerName,
        releaseDate = this.releaseDate,
        actors = actors.toMutableList(),
        id = this.id
    )

fun ResultRow.toMovieWithProducingActorDTO() = MovieWithProducingActorDTO(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
