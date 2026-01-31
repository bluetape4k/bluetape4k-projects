package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toCollection
import org.eclipse.collections.impl.list.mutable.FastList
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList
import org.eclipse.collections.impl.set.mutable.UnifiedSet

suspend fun <T> Flow<T>.toFastList(destination: FastList<T> = FastList.newList<T>()): FastList<T> =
    toCollection(destination)

suspend fun <T> Flow<T>.toUnifiedSet(destination: UnifiedSet<T> = UnifiedSet.newSet<T>()): UnifiedSet<T> =
    toCollection(destination)

@JvmName("toIntArrayList")
suspend fun Flow<Int>.toArrayList(initialCapacity: Int = INITIAL_CAPACITY): IntArrayList =
    IntArrayList(initialCapacity).also { array ->
        this@toArrayList.collect {
            array.add(it)
        }
    }

@JvmName("toLongArrayList")
suspend fun Flow<Long>.toArrayList(initialCapacity: Int = INITIAL_CAPACITY): LongArrayList =
    LongArrayList(initialCapacity).also { array ->
        this@toArrayList.collect {
            array.add(it)
        }
    }

@JvmName("toFloatArrayList")
suspend fun Flow<Float>.toArrayList(initialCapacity: Int = INITIAL_CAPACITY): FloatArrayList =
    FloatArrayList(initialCapacity).also { array ->
        this@toArrayList.collect {
            array.add(it)
        }
    }

@JvmName("toDoubleArrayList")
suspend fun Flow<Double>.toArrayList(initialCapacity: Int = INITIAL_CAPACITY): DoubleArrayList =
    DoubleArrayList(initialCapacity).also { array ->
        this@toArrayList.collect {
            array.add(it)
        }
    }
