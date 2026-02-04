package io.bluetape4k.exposed.r2dbc.domain.repository

import io.bluetape4k.exposed.r2dbc.domain.model.ActorRecord
import io.bluetape4k.exposed.r2dbc.domain.model.MovieSchema.ActorTable
import io.bluetape4k.exposed.r2dbc.domain.model.toActorRecord
import io.bluetape4k.exposed.r2dbc.repository.ExposedR2dbcRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.insertAndGetId
import org.jetbrains.exposed.v1.r2dbc.selectAll
import java.time.LocalDate

class ActorR2dbcRepository: ExposedR2dbcRepository<ActorRecord, Long> {

    companion object: KLoggingChannel()

    override val table = ActorTable
    override suspend fun ResultRow.toEntity(): ActorRecord = toActorRecord()

    fun searchActors(params: Map<String, String?>): Flow<ActorRecord> {
        val query = ActorTable.selectAll()

        params.forEach { (key, value) ->
            when (key) {
                ActorTable::id.name -> value?.run { query.andWhere { ActorTable.id eq value.toLong() } }
                ActorTable::firstName.name -> value?.run { query.andWhere { ActorTable.firstName eq value } }
                ActorTable::lastName.name -> value?.run { query.andWhere { ActorTable.lastName eq value } }
                ActorTable::birthday.name -> value?.run { query.andWhere { ActorTable.birthday eq LocalDate.parse(value) } }
            }
        }

        return query.map { it.toEntity() }
    }

    suspend fun save(actor: ActorRecord): ActorRecord {
        log.debug { "Create new actor. actor: $actor" }

        val id = ActorTable.insertAndGetId {
            it[firstName] = actor.firstName
            it[lastName] = actor.lastName
            it[birthday] = actor.birthday?.let { day -> LocalDate.parse(day) }
        }
        return actor.copy(id = id.value)
    }
}
