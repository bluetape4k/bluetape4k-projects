package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import org.eclipse.collections.impl.set.mutable.UnifiedSet

suspend fun <T> Flow<T>.toFastList(): FastList<T> =
    FastList<T>().also { list ->
        collect { element ->
            list.add(element)
        }
    }

suspend fun <T> Flow<T>.toUnifiedSet(): UnifiedSet<T> =
    UnifiedSet<T>().also { set ->
        collect { element ->
            set.add(element)
        }
    }

@JvmName("toIntArrayList")
suspend fun Flow<Int>.toArrayList(initialCapacity: Int = INITIAL_CAPACITY): IntArrayList =
    IntArrayList(initialCapacity).also { array ->
        collect {
            array.add(it)
        }
    }

@JvmName("toLongArrayList")
suspend fun Flow<Long>.toArrayList(initialCapacity: Int = INITIAL_CAPACITY): LongArrayList =
    LongArrayList(initialCapacity).also { array ->
        collect {
            array.add(it)
        }
    }

@JvmName("toFloatArrayList")
suspend fun Flow<Float>.toArrayList(initialCapacity: Int = INITIAL_CAPACITY): FloatArrayList =
    FloatArrayList(initialCapacity).also { array ->
        collect {
            array.add(it)
        }
    }

@JvmName("toDoubleArrayList")
suspend fun Flow<Double>.toArrayList(initialCapacity: Int = INITIAL_CAPACITY): DoubleArrayList =
    DoubleArrayList(initialCapacity).also { array ->
        collect {
            array.add(it)
        }
    }
