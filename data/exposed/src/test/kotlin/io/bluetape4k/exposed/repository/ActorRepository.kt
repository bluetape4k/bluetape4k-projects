package io.bluetape4k.exposed.repository

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.exposed.domain.model.ActorRecord
import io.bluetape4k.exposed.domain.model.MovieSchema.ActorEntity
import io.bluetape4k.exposed.domain.model.MovieSchema.ActorTable
import io.bluetape4k.exposed.domain.model.toActorRecord
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate

class ActorRepository: ExposedRepository<ActorRecord, Long> {

    companion object: KLogging()

    override val table = ActorTable

    override fun ResultRow.toEntity(): ActorRecord = toActorRecord()

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

        return ActorEntity.wrapRows(query).toFastList()
    }

    fun save(actor: ActorRecord): ActorRecord {
        log.debug { "Create new actor. actor: $actor" }

        val id = ActorTable.insertAndGetId {
            it[firstName] = actor.firstName
            it[lastName] = actor.lastName
            it[birthday] = actor.birthday?.let { birthday -> LocalDate.parse(birthday) }
        }
        return actor.copy(id = id.value)
    }
}
