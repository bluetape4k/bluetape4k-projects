package io.bluetape4k.collections.eclipse

import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.set.mutable.UnifiedSet

fun CharSequence.toFastList(destination: MutableList<Char> = FastList.newList()): List<Char> =
    toCollection(destination)


fun CharSequence.toUnifiedSet(destination: MutableSet<Char> = UnifiedSet.newSet()): Set<Char> =
    toCollection(destination)
