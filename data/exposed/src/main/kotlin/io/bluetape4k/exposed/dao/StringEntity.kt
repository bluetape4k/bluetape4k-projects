package io.bluetape4k.exposed.dao

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

/**
 * Base class for an [Entity] instance identified by an [id] comprised of a wrapped `String` value.
 */
abstract class StringEntity(id: EntityID<String>): Entity<String>(id)


/**
 * Base class representing the [EntityClass] that manages [StringEntity] instances and
 * maintains their relation to the provided [table].
 *
 * @param [table] The [IdTable] object that stores rows mapped to entities of this class.
 * @param [entityType] The expected [StringEntity] type. This can be left `null` if it is the class of type
 * argument [E] provided to this [StringEntityClass] instance. If this `StringEntityClass` is defined as a companion
 * object of a custom `StringEntity` class, the parameter will be set to this immediately enclosing class by default.
 * @sample org.jetbrains.exposed.sql.tests.shared.DDLTests.testDropTableFlushesCache
 * @param [entityCtor] The function invoked to instantiate a [StringEntity] using a provided [EntityID] value.
 * If a reference to a specific constructor or a custom function is not passed as an argument, reflection will
 * be used to determine the primary constructor of the associated entity class on first access. If this `StringEntityClass`
 * is defined as a companion object of a custom `StringEntity` class, the constructor will be set to that of the
 * immediately enclosing class by default.
 * @sample org.jetbrains.exposed.sql.tests.shared.entities.EntityTests.testExplicitEntityConstructor
 */
abstract class StringEntityClass<out E: StringEntity>(
    table: IdTable<String>,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null,
): EntityClass<String, E>(table, entityType, entityCtor)
