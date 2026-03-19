package io.bluetape4k.javers.diff

import org.javers.core.diff.Change
import org.javers.core.diff.Diff

/**
 * [Diff]에서 지정한 변경 타입에 해당하는 affected 객체 목록을 반환한다.
 *
 * ```kotlin
 * val removedObjects = diff.objectsByChangeType<ObjectRemoved>()
 * // removedObjects == [Employee("To Be Fired")]
 * ```
 */
inline fun <reified T: Change> Diff.objectsByChangeType(): MutableList<Any?> =
    getObjectsByChangeType(T::class.java)

/**
 * [Diff]에서 지정한 타입의 변경 목록을 반환한다.
 *
 * ```kotlin
 * val valueChanges = diff.changesByType<ValueChange>()
 * // valueChanges.all { it is ValueChange } == true
 * ```
 */
inline fun <reified T: Change> Diff.changesByType(): MutableList<T> =
    getChangesByType(T::class.java)
