package io.bluetape4k.javers.diff

import org.javers.core.diff.Change
import org.javers.core.diff.Diff

inline fun <reified T: Change> Diff.objectsByChangeType(): MutableList<Any?> =
    getObjectsByChangeType(T::class.java)

inline fun <reified T: Change> Diff.changesByType(): MutableList<T> =
    getChangesByType(T::class.java)
