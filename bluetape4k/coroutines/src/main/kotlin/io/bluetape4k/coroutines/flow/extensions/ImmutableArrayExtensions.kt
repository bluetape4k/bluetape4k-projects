package io.bluetape4k.coroutines.flow.extensions

import com.danrusu.pods4k.immutableArrays.ImmutableArray
import com.danrusu.pods4k.immutableArrays.ImmutableByteArray
import com.danrusu.pods4k.immutableArrays.ImmutableCharArray
import com.danrusu.pods4k.immutableArrays.ImmutableDoubleArray
import com.danrusu.pods4k.immutableArrays.ImmutableFloatArray
import com.danrusu.pods4k.immutableArrays.ImmutableIntArray
import com.danrusu.pods4k.immutableArrays.ImmutableLongArray
import com.danrusu.pods4k.immutableArrays.ImmutableShortArray
import kotlinx.coroutines.flow.Flow

internal const val INITIAL_CAPACITY = 10

@JvmName("toImmutableArrayFromAny")
suspend fun <T> Flow<T>.toImmutableArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableArray<T> =
    ImmutableArray.Builder<T>(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()


@JvmName("toImmutableByteArrayFromByte")
suspend fun Flow<Byte>.toImmutableArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableByteArray =
    ImmutableByteArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

@JvmName("toImmutableCharArrayFromChar")
suspend fun Flow<Char>.toImmutableArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableCharArray =
    ImmutableCharArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

@JvmName("toImmutableShortArrayFromShort")
suspend fun Flow<Short>.toImmutableArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableShortArray =
    ImmutableShortArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

@JvmName("toImmutableIntArrayFromInt")
suspend fun Flow<Int>.toImmutableArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableIntArray =
    ImmutableIntArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

@JvmName("toImmutableLongArrayFromLong")
suspend fun Flow<Long>.toImmutableArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableLongArray =
    ImmutableLongArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()


@JvmName("toImmutableFloatArrayFromFloat")
suspend fun Flow<Float>.toImmutableArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableFloatArray =
    ImmutableFloatArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

@JvmName("toImmutableDoubleArrayFromDouble")
suspend fun Flow<Double>.toImmutableArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableDoubleArray =
    ImmutableDoubleArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

@JvmName("toImmutableShortArrayFromNumber")
suspend fun Flow<Number>.toImmutableShortArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableShortArray =
    ImmutableShortArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it.toShort())
            }
        }
        .build()


@JvmName("toImmutableIntArrayFromNumber")
suspend fun Flow<Number>.toImmutableIntArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableIntArray =
    ImmutableIntArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it.toInt())
            }
        }
        .build()

@JvmName("toImmutableLongArrayFromNumber")
suspend fun Flow<Number>.toImmutableLongArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableLongArray =
    ImmutableLongArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it.toLong())
            }
        }
        .build()

@JvmName("toImmutableFloatArrayFromNumber")
suspend fun Flow<Number>.toImmutableFloatArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableFloatArray =
    ImmutableFloatArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it.toFloat())
            }
        }
        .build()


@JvmName("toImmutableDoubleArrayFromNumber")
suspend fun Flow<Number>.toImmutableDoubleArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableDoubleArray =
    ImmutableDoubleArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it.toDouble())
            }
        }
        .build()
