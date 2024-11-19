package io.bluetape4k.cassandra.cql

import com.datastax.oss.driver.api.core.type.DataType
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.core.type.ListType
import com.datastax.oss.driver.api.core.type.MapType
import com.datastax.oss.driver.api.core.type.SetType
import com.datastax.oss.driver.api.core.type.UserDefinedType

/**
 * Cassandra [DataType]이 Collection Type인지 확인합니다. ([ListType], [SetType], [MapType])
 */
val DataType.isCollectionType: Boolean
    get() = this is ListType || this is SetType || this is MapType

/**
 * Cassandra [DataType]이 Non-Frozen UDT인지 확인합니다.
 */
val DataType.isNonFrozenUdt: Boolean
    get() = this is UserDefinedType && !this.isFrozen


/**
 * Cassandra [DataType]을 Freeze 할 수 있는지 확인하고, Freeze 할 수 있다면 Freeze 합니다.
 */
fun DataType.potentiallyFreeze(): DataType {
    when (this) {
        is ListType -> {
            if (elementType.isCollectionType || elementType.isNonFrozenUdt) {
                return DataTypes.listOf(elementType.potentiallyFreeze(), this.isFrozen)
            }
        }

        is SetType  -> {
            if (elementType.isCollectionType || elementType.isNonFrozenUdt) {
                return DataTypes.setOf(elementType.potentiallyFreeze(), isFrozen)
            }
        }

        is MapType  -> {
            if (keyType.isCollectionType || valueType.isCollectionType || keyType.isNonFrozenUdt || valueType.isNonFrozenUdt) {
                return DataTypes.mapOf(keyType.potentiallyFreeze(), valueType.potentiallyFreeze(), isFrozen)
            }
        }
    }

    if (isNonFrozenUdt) {
        return (this as UserDefinedType).copy(true)
    }
    return this
}
