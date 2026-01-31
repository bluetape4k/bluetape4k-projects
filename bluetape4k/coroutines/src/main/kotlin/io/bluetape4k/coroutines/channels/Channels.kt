package io.bluetape4k.coroutines.channels

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.set.mutable.UnifiedSet

suspend fun <E> ReceiveChannel<E>.toFastList(): FastList<E> {
    return FastList.newList<E>().also { list ->
        this@toFastList.consumeEach {
            list.add(it)
        }
    }
}

suspend fun <E> ReceiveChannel<E>.toUnifiedSet(): UnifiedSet<E> {
    return UnifiedSet.newSet<E>().also { list ->
        this@toUnifiedSet.consumeEach {
            list.add(it)
        }
    }
}
