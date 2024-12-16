package io.bluetape4k.coroutines.flow

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

suspend fun <T> Flow<T>.toImmutableArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableArray<T> =
    ImmutableArray.Builder<T>(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()


suspend fun Flow<Byte>.toImmutableByteArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableByteArray =
    ImmutableByteArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

suspend fun Flow<Char>.toImmutableCharArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableCharArray =
    ImmutableCharArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

suspend fun Flow<Int>.toImmutableIntArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableIntArray =
    ImmutableIntArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

suspend fun Flow<Long>.toImmutableLongArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableLongArray =
    ImmutableLongArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

suspend fun Flow<Short>.toImmutableShortArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableShortArray =
    ImmutableShortArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

suspend fun Flow<Float>.toImmutableFloatArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableFloatArray =
    ImmutableFloatArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()

suspend fun Flow<Double>.toImmutableDoubleArray(initialCapacity: Int = INITIAL_CAPACITY): ImmutableDoubleArray =
    ImmutableDoubleArray.Builder(initialCapacity)
        .apply {
            collect {
                add(it)
            }
        }
        .build()
