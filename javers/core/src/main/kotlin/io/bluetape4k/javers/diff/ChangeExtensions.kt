package io.bluetape4k.javers.diff

import org.javers.core.Changes
import org.javers.core.diff.Change
import org.javers.core.diff.changetype.NewObject
import org.javers.core.diff.changetype.ObjectRemoved
import org.javers.core.diff.changetype.ReferenceChange
import org.javers.core.diff.changetype.ValueChange
import org.javers.core.diff.changetype.container.ArrayChange
import org.javers.core.diff.changetype.container.ListChange
import org.javers.core.diff.changetype.container.SetChange
import org.javers.core.diff.changetype.map.MapChange

inline fun <reified T: Change> Changes.filterByType(): List<T> =
    this.getChangesByType(T::class.java)

val Change.isArrayChange: Boolean get() = this is ArrayChange
val Change.isListChange: Boolean get() = this is ListChange
val Change.isMapChange: Boolean get() = this is MapChange<*>
val Change.isSetChange: Boolean get() = this is SetChange
val Change.isReferenceChange: Boolean get() = this is ReferenceChange
val Change.isValueChange: Boolean get() = this is ValueChange

val Change.isNewObject: Boolean get() = this is NewObject
val Change.isObjectRemoved: Boolean get() = this is ObjectRemoved
