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

/**
 * [Changes]에서 지정한 타입의 변경만 필터링하여 반환한다.
 *
 * ```kotlin
 * val valueChanges = changes.filterByType<ValueChange>()
 * // valueChanges.all { it is ValueChange } == true
 * ```
 */
inline fun <reified T: Change> Changes.filterByType(): List<T> =
    this.getChangesByType(T::class.java)

/** 배열 변경 여부 */
val Change.isArrayChange: Boolean get() = this is ArrayChange

/** 리스트 변경 여부 */
val Change.isListChange: Boolean get() = this is ListChange

/** 맵 변경 여부 */
val Change.isMapChange: Boolean get() = this is MapChange<*>

/** 셋 변경 여부 */
val Change.isSetChange: Boolean get() = this is SetChange

/** 참조 변경 여부 */
val Change.isReferenceChange: Boolean get() = this is ReferenceChange

/** 값 변경 여부 */
val Change.isValueChange: Boolean get() = this is ValueChange

/** 새 객체 생성 여부 */
val Change.isNewObject: Boolean get() = this is NewObject

/** 객체 삭제 여부 */
val Change.isObjectRemoved: Boolean get() = this is ObjectRemoved
