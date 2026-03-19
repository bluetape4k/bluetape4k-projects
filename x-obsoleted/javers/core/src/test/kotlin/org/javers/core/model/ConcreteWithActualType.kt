package org.javers.core.model

import org.javers.core.metamodel.annotation.Id
import java.io.Serializable

abstract class AbstractGeneric<ID, V>(
    @Id var id: ID,
    var value: V,
): Serializable

class ConcreteWithActualType(
    id: String,
    value: List<String>,
): AbstractGeneric<String, List<String>>(id, value)
