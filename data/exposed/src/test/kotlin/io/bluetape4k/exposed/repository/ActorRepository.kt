package io.bluetape4k.exposed.repository

import io.bluetape4k.exposed.domain.dto.ActorDTO
import io.bluetape4k.exposed.domain.mapper.toActorDTO
import io.bluetape4k.exposed.domain.model.MovieSchema.ActorEntity
import io.bluetape4k.exposed.domain.model.MovieSchema.ActorTable
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate

class ActorRepository: ExposedRepository<ActorDTO, Long> {

    companion object: KLogging()

    override val table = ActorTable

    override fun ResultRow.toEntity(): ActorDTO = toActorDTO()

    fun searchActors(params: Map<String, String?>): List<ActorEntity> {
        val query = ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                ActorTable::id.name -> value?.run { query.andWhere { ActorTable.id eq value.toLong() } }
                ActorTable::firstName.name -> value?.run { query.andWhere { ActorTable.firstName eq value } }
                ActorTable::lastName.name -> value?.run { query.andWhere { ActorTable.lastName eq value } }
                ActorTable::birthday.name -> value?.run { query.andWhere { ActorTable.birthday eq LocalDate.parse(value) } }
            }
        }

        return ActorEntity.wrapRows(query).toList()
    }

    fun save(actor: ActorDTO): ActorDTO {
        log.debug { "Create new actor. actor: $actor" }

        val id = ActorTable.insertAndGetId {
            it[firstName] = actor.firstName
            it[lastName] = actor.lastName
            it[birthday] = actor.birthday?.let { LocalDate.parse(it) }
        }
        return actor.copy(id = id.value)
    }
}
