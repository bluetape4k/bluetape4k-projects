package io.bluetape4k.examples.redisson.coroutines.cachestrategy

import io.bluetape4k.exposed.dao.id.SnowflakeIdEntity
import io.bluetape4k.exposed.dao.id.SnowflakeIdEntityClass
import io.bluetape4k.exposed.dao.id.SnowflakeIdTable
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import java.io.Serializable

object ActorSchema {

    object ActorTable: SnowflakeIdTable("exposed_actors") {
        val firstname = varchar("first_name", 50)
        val lastname = varchar("last_name", 50)
        val description = text("description").nullable()

        init {
            uniqueIndex("idx_actor_full_name", firstname, lastname)
        }
    }

    class ActorEntity(id: EntityID<Long>): SnowflakeIdEntity(id), Serializable {
        companion object: SnowflakeIdEntityClass<ActorEntity>(ActorTable)

        var firstname by ActorTable.firstname
        var lastname by ActorTable.lastname
        var description by ActorTable.description

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("firstname", firstname)
            .add("lastname", lastname)
            .toString()
    }

    data class Actor(
        val id: Long,
        val firstname: String,
        val lastname: String,
    ): Serializable {
        var description: String? = null
    }

    fun ResultRow.toActor(): Actor =
        Actor(
            id = this[ActorTable.id].value,
            firstname = this[ActorTable.firstname],
            lastname = this[ActorTable.lastname],
        ).also {
            it.description = this@toActor[ActorTable.description]
        }

    fun ActorEntity.toActor(): Actor =
        Actor(
            id = this.id.value,
            firstname = this.firstname,
            lastname = this.lastname,
        ).also {
            it.description = this@toActor.description
        }
}
