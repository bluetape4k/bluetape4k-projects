package org.javers.core.model

import org.javers.core.metamodel.annotation.Entity
import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.annotation.TypeName
import java.io.Serializable
import java.math.BigDecimal

@TypeName("org.javers.core.model.OldEntity")
data class NewEntity(
    @Id val id: Int,
    val value: Int = 0,
    val newValue: Int = 0,
): Serializable

@TypeName("myName")
@Entity
class NewEntityWithTypeAlias(
    @Id var id: BigDecimal,
): Serializable {
    var value: Int = 0
    var valueObject: NewValueObjectWithTypeAlias? = null
}

@TypeName("myValueObject")
class NewValueObjectWithTypeAlias(
    var some: Int,
): Serializable
