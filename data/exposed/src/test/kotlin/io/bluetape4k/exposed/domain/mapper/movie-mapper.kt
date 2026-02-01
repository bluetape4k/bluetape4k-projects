package io.bluetape4k.exposed.domain.mapper

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.exposed.domain.dto.ActorDTO
import io.bluetape4k.exposed.domain.dto.MovieDTO
import io.bluetape4k.exposed.domain.dto.MovieWithActorDTO
import io.bluetape4k.exposed.domain.dto.MovieWithProducingActorDTO
import io.bluetape4k.exposed.domain.model.MovieSchema
import io.bluetape4k.exposed.domain.model.MovieSchema.ActorEntity
import io.bluetape4k.exposed.domain.model.MovieSchema.ActorTable
import io.bluetape4k.exposed.domain.model.MovieSchema.MovieEntity
import io.bluetape4k.exposed.domain.model.MovieSchema.MovieTable
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toActorDTO() = ActorDTO(
    id = this[ActorTable.id].value,
    firstName = this[ActorTable.firstName],
    lastName = this[ActorTable.lastName],
    birthday = this[ActorTable.birthday]?.toString()
)

fun ActorEntity.toActorDTO() = ActorDTO(
    id = this.id.value,
    firstName = this.firstName,
    lastName = this.lastName,
    birthday = this.birthday?.toString()
)

fun ResultRow.toMovieDTO() = MovieDTO(
    name = this[MovieSchema.MovieTable.name],
    producerName = this[MovieSchema.MovieTable.producerName],
    releaseDate = this[MovieSchema.MovieTable.releaseDate].toString(),
    id = this[MovieSchema.MovieTable.id].value
)

fun ResultRow.toMovieWithActorDTO(actors: List<ActorDTO>) = MovieWithActorDTO(
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    actors = actors.toFastList(),
    id = this[MovieTable.id].value
)

fun MovieDTO.toMovieWithActorDTO(actors: List<ActorDTO>) = MovieWithActorDTO(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors.toFastList(),
    id = this.id
)

fun MovieEntity.toMovieDTO() = MovieDTO(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    id = this.id.value
)

fun MovieEntity.toMovieWithActorDTO() = MovieWithActorDTO(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorDTO() }.toFastList(),
    id = this.id.value
)


fun ResultRow.toMovieWithProducingActorDTO() = MovieWithProducingActorDTO(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
