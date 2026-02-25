package io.bluetape4k.exposed.dao.id

import io.bluetape4k.exposed.core.dao.id.KsuidTable
import io.bluetape4k.exposed.dao.StringEntity
import io.bluetape4k.exposed.dao.StringEntityClass
import org.jetbrains.exposed.v1.core.dao.id.EntityID


typealias KsuidEntityID = EntityID<String>

open class KsuidEntity(id: KsuidEntityID): StringEntity(id)

open class KsuidEntityClass<out E: KsuidEntity>(
    table: KsuidTable,
    entityType: Class<E>? = null,
    entityCtor: ((KsuidEntityID) -> E)? = null,
): StringEntityClass<E>(table, entityType, entityCtor)
