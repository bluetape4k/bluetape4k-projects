package io.bluetape4k.examples.redisson.coroutines.cachestrategy

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import java.io.Serializable

object ActorSchema {

    object ActorTable: LongIdTable("exposed_actors") {
        val firstname = varchar("first_name", 50)
        val lastname = varchar("last_name", 50)
        val description = text("description").nullable()

        init {
            uniqueIndex("idx_actor_full_name", firstname, lastname)
        }
    }

    class ActorEntity(id: EntityID<Long>): LongEntity(id), Serializable {
        companion object: LongEntityClass<ActorEntity>(ActorTable)

        var firstname by ActorTable.firstname
        var lastname by ActorTable.lastname
        var description by ActorTable.description

        override fun equals(other: Any?): Boolean = other is ActorEntity && id == other.id
        override fun hashCode(): Int = id.value.hashCode()
        override fun toString(): String = "ActorEntity(id=${id.value}, firstname=$firstname, lastname=$lastname)"
    }

    data class ActorRecord(
        val id: Long,
        val firstname: String,
        val lastname: String,
    ): Serializable {
        var description: String? = null
        fun withId(id: Long) = copy(id = id)
    }

    fun ResultRow.toActorRecord(): ActorRecord =
        ActorRecord(
            id = this[ActorTable.id].value,
            firstname = this[ActorTable.firstname],
            lastname = this[ActorTable.lastname],
        ).also {
            it.description = this@toActorRecord[ActorTable.description]
        }

    fun ActorEntity.toActorRecord(): ActorRecord =
        ActorRecord(
            id = this.id.value,
            firstname = this.firstname,
            lastname = this.lastname,
        ).also {
            it.description = this@toActorRecord.description
        }
}
