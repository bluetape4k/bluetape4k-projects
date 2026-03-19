package io.bluetape4k.javers.metamodel

import org.javers.core.metamodel.`object`.GlobalId
import org.javers.core.metamodel.`object`.InstanceId
import org.javers.core.metamodel.`object`.ValueObjectId
import org.javers.core.metamodel.type.EntityType
import org.javers.core.metamodel.type.ManagedType

/**
 * 현재 [GlobalId]가 [childCandidate]의 부모인지 확인한다.
 *
 * ## 동작/계약
 * - 현재 ID가 [InstanceId]이고 [childCandidate]가 [ValueObjectId]이며, 그 소유자가 현재 ID인 경우 true
 * - 그 외에는 false
 */
fun GlobalId.isParent(childCandidate: GlobalId): Boolean {
    if (this !is InstanceId || childCandidate !is ValueObjectId) {
        return false
    }
    return childCandidate.ownerId == this
}

/**
 * 현재 [GlobalId]가 [parentCandidate] 타입의 자식인지 확인한다.
 *
 * ## 동작/계약
 * - [parentCandidate]가 [EntityType]이고 현재 ID가 [ValueObjectId]이며, 그 소유자가 해당 엔티티 타입인 경우 true
 * - 그 외에는 false
 */
fun GlobalId.isChild(parentCandidate: ManagedType): Boolean {
    if (parentCandidate !is EntityType || this !is ValueObjectId) {
        return false
    }
    return this.ownerId == parentCandidate
}
