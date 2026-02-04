package io.bluetape4k.exposed.shared.entities

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.exposed.shared.entities.MovieSchema.ActorEntity
import io.bluetape4k.exposed.shared.entities.MovieSchema.ActorTable
import io.bluetape4k.exposed.shared.entities.MovieSchema.MovieEntity
import io.bluetape4k.exposed.shared.entities.MovieSchema.MovieTable
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toActorRecord() = ActorRecord(
    id = this[ActorTable.id].value,
    firstName = this[ActorTable.firstName],
    lastName = this[ActorTable.lastName],
    birthday = this[ActorTable.birthday]?.toString()
)

fun ActorEntity.toActorRecord() = ActorRecord(
    id = this.id.value,
    firstName = this.firstName,
    lastName = this.lastName,
    birthday = this.birthday?.toString()
)

fun ResultRow.toMovieRecord() = MovieRecord(
    name = this[MovieSchema.MovieTable.name],
    producerName = this[MovieSchema.MovieTable.producerName],
    releaseDate = this[MovieSchema.MovieTable.releaseDate].toString(),
    id = this[MovieSchema.MovieTable.id].value
)

fun ResultRow.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    name = this[MovieTable.name],
    producerName = this[MovieTable.producerName],
    releaseDate = this[MovieTable.releaseDate].toString(),
    actors = actors.toFastList(),
    id = this[MovieTable.id].value
)

fun MovieRecord.toMovieWithActorRecord(actors: List<ActorRecord>) = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate,
    actors = actors.toFastList(),
    id = this.id
)

fun MovieEntity.toMovieRecord() = MovieRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    id = this.id.value
)

fun MovieEntity.toMovieWithActorRecord() = MovieWithActorRecord(
    name = this.name,
    producerName = this.producerName,
    releaseDate = this.releaseDate.toString(),
    actors = this.actors.map { it.toActorRecord() }.toFastList(),
    id = this.id.value
)


fun ResultRow.toMovieWithProducingActorRecord() = MovieWithProducingActorRecord(
    movieName = this[MovieTable.name],
    producerActorName = this[ActorTable.firstName] + " " + this[ActorTable.lastName]
)
