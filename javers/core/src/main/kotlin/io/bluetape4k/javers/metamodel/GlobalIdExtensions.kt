package io.bluetape4k.javers.metamodel

import org.javers.core.metamodel.`object`.GlobalId
import org.javers.core.metamodel.`object`.InstanceId
import org.javers.core.metamodel.`object`.ValueObjectId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ManagedType

fun GlobalId.isParent(childCandidate: GlobalId): Boolean {
    if (this !is InstanceId || childCandidate !is ValueObjectId) {
        return false
    }
    return childCandidate.ownerId == this
}

fun GlobalId.isChild(parentCandidate: ManagedType): Boolean {
    if (parentCandidate !is EntityType || this !is ValueObjectId) {
        return false
    }
    return this.ownerId == parentCandidate
}
