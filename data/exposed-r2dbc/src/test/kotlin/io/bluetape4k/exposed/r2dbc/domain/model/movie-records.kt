package io.bluetape4k.exposed.r2dbc.domain.model

import io.bluetape4k.exposed.core.HasIdentifier
import java.io.Serializable


/**
 * 영화 정보를 나타내는 DTO
 */
data class MovieRecord(
    override val id: Long,
    val name: String,
    val producerName: String,
    val releaseDate: String,
): HasIdentifier<Long> {
    fun withId(id: Long) = copy(id = id)
}

/**
 * 영화 배우 정보를 담는 DTO
 */
data class ActorRecord(
    override val id: Long,
    val firstName: String,
    val lastName: String,
    val birthday: String? = null,
): HasIdentifier<Long> {
    fun withId(id: Long) = copy(id = id)
}

/**
 * 영화 배우 정보와 해당 배우가 출연한 영화 정보를 나타내는 DTO
 */
data class MovieActorRecord(
    val movieId: Long,
    val actorId: Long,
): Serializable


/**
 * 영화 제목과 영화에 출연한 배우의 수를 나타내는 DTO
 */
data class MovieActorCountRecord(
    val movieName: String,
    val actorCount: Int,
): Serializable


/**
 * 영화 정보와 해당 영화에 출연한 배우 정보를 나타내는 DTO
 */
data class MovieWithActorRecord(
    val name: String,
    val producerName: String,
    val releaseDate: String,
    val actors: List<ActorRecord> = emptyList(),
    val id: Long? = null,
): Serializable


/**
 * 영화 제목과 영화를 제작한 배우의 이름을 나타내는 DTO
 */
data class MovieWithProducingActorRecord(
    val movieName: String,
    val producerActorName: String,
): Serializable
