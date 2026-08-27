package io.bluetape4k.science.exposed.service.internal

import io.bluetape4k.science.exposed.NetCdfException

/** NetCDF import 에 적용하는 bounded resource 계약입니다. */
internal const val MAX_VARIABLE_NAME_BYTES: Long = 128L
internal const val MAX_COORDINATE_TOKENS: Long = 32L
internal const val MAX_AUXILIARY_AXES: Long = 16L
internal const val MAX_VARIABLES: Long = 1_024L
internal const val MAX_GROUP_DIMENSIONS: Long = 256L
internal const val MAX_GROUP_COUNT: Long = 256L
internal const val MAX_GROUP_DEPTH: Long = 32L
internal const val MAX_METADATA_BYTES: Long = 1L shl 20
internal const val MAX_FILE_BYTES: Long = 64L * 1024L * 1024L * 1024L
internal const val MAX_CELLS: Long = 100_000_000L
internal const val MAX_SLICES: Long = 1_000_000L
internal const val MAX_TILE_CELLS: Long = 65_536L
internal const val MAX_BATCH_ROWS: Long = 1_000L
internal const val MAX_AUXILIARY_JSONB_BYTES: Long = 8_192L
internal const val MAX_DUPLICATE_ENTRY_BYTES: Long = 32L
internal const val MAX_COORDINATE_CACHE_BYTES: Long = 64L * 1024L * 1024L
internal const val MAX_COORDINATE_WORKING_SET_BYTES: Long = 64L * 1024L * 1024L
internal const val MAX_DUPLICATE_SET_BYTES: Long = 32L * 1024L * 1024L
internal const val MAX_OWNED_WORKING_SET_BYTES: Long = 128L * 1024L * 1024L
internal const val MAX_FIXED_ROW_BYTES: Long = 256L
internal const val MAX_LEASE_RETRIES: Int = 3

/** checked Long 곱셈을 수행하고 overflow 를 typed resource 오류로 변환합니다. */
internal fun checkedProduct(vararg factors: Long): Long {
    if (factors.any { it < 0L }) {
        val actual = factors.firstOrNull { it < 0L } ?: -1L
        throw NetCdfException.ResourceLimitExceeded("negative-factor", 0L, actual)
    }
    return try {
        factors.fold(1L) { acc, factor -> Math.multiplyExact(acc, factor) }
    } catch (e: ArithmeticException) {
        throw NetCdfException.ResourceLimitExceeded("long-product", Long.MAX_VALUE, Long.MAX_VALUE,)
    }
}

/** checked Long 덧셈입니다. */
internal fun checkedSum(vararg values: Long): Long {
    if (values.any { it < 0L }) {
        val actual = values.firstOrNull { it < 0L } ?: -1L
        throw NetCdfException.ResourceLimitExceeded("negative-value", 0L, actual)
    }
    return try {
        values.fold(0L) { acc, value -> Math.addExact(acc, value) }
    } catch (e: ArithmeticException) {
        throw NetCdfException.ResourceLimitExceeded("long-sum", Long.MAX_VALUE, Long.MAX_VALUE)
    }
}

/** import 한 번이 소유할 수 있는 working-set byte accounting 입니다. */
internal data class MemoryBudget(
    val tileBufferBytes: Long,
    val coordinateBytes: Long,
    val serializerScratchBytes: Long,
    val duplicateSetBytes: Long,
) {
    val ownedWorkingSetBytes: Long
        get() = checkedSum(tileBufferBytes, coordinateBytes, serializerScratchBytes, duplicateSetBytes)

    fun requireWithinLimit() {
        if (coordinateBytes > MAX_COORDINATE_CACHE_BYTES) {
            throw NetCdfException.ResourceLimitExceeded(
                "coordinate-cache",
                MAX_COORDINATE_CACHE_BYTES,
                coordinateBytes,
            )
        }
        val coordinateAndDuplicate = checkedSum(coordinateBytes, duplicateSetBytes)
        if (coordinateAndDuplicate > MAX_COORDINATE_WORKING_SET_BYTES) {
            throw NetCdfException.ResourceLimitExceeded(
                "coordinate-working-set",
                MAX_COORDINATE_WORKING_SET_BYTES,
                coordinateAndDuplicate,
            )
        }
        val actual = ownedWorkingSetBytes
        if (actual > MAX_OWNED_WORKING_SET_BYTES) {
            throw NetCdfException.ResourceLimitExceeded(
                "owned-working-set",
                MAX_OWNED_WORKING_SET_BYTES,
                actual,
            )
        }
    }
}

/** tile 안에서 duplicate canonical coordinate 를 bounded 하게 추적합니다. */
internal class CoordinateKeySet(
    private val byteBudget: Long = MAX_DUPLICATE_SET_BYTES,
    expectedSize: Int? = null,
) {
    private var timeLevels: LongArray
    private var longitudes: LongArray
    private var latitudes: LongArray
    private var used: BooleanArray
    private var count: Int = 0

    init {
        val capacity = expectedSize?.let(::capacityFor) ?: INITIAL_CAPACITY
        requireCapacityWithinBudget(capacity)
        timeLevels = LongArray(capacity)
        longitudes = LongArray(capacity)
        latitudes = LongArray(capacity)
        used = BooleanArray(capacity)
    }

    /** open-addressing backing table과 rehash 여유를 반영한 byte estimate 입니다. */
    val estimatedBytes: Long get() = checkedProduct(used.size.toLong(), BYTES_PER_SLOT)

    fun add(timeIdx: Int, levelIdx: Int, longitude: Double, latitude: Double): Boolean {
        val longitudeBits = canonicalDoubleBits(longitude)
        val latitudeBits = canonicalDoubleBits(latitude)
        if (count + 1 > used.size * LOAD_FACTOR_NUMERATOR / LOAD_FACTOR_DENOMINATOR) {
            ensureCapacity(count + 1)
        }
        val packedTimeLevel = packTimeLevel(timeIdx, levelIdx)
        val hash = hash(packedTimeLevel, longitudeBits, latitudeBits)
        var slot = slotFor(hash, used.size)
        while (used[slot]) {
            if (timeLevels[slot] == packedTimeLevel &&
                longitudes[slot] == longitudeBits && latitudes[slot] == latitudeBits
            ) {
                return false
            }
            slot = (slot + 1) % used.size
        }
        used[slot] = true
        timeLevels[slot] = packedTimeLevel
        longitudes[slot] = longitudeBits
        latitudes[slot] = latitudeBits
        count++
        return true
    }

    fun size(): Int = count

    private fun ensureCapacity(required: Int) {
        if (required * LOAD_FACTOR_DENOMINATOR <= used.size * LOAD_FACTOR_NUMERATOR) return
        val oldUsed = used
        val oldTimeLevels = timeLevels
        val oldLongitudes = longitudes
        val oldLatitudes = latitudes
        val newCapacity = capacityFor(required)
        val rehashBytes = checkedSum(
            checkedProduct(oldUsed.size.toLong(), BYTES_PER_SLOT),
            checkedProduct(newCapacity.toLong(), BYTES_PER_SLOT),
        )
        if (rehashBytes > byteBudget) {
            throw NetCdfException.ResourceLimitExceeded("duplicate-coordinate-set", byteBudget, rehashBytes)
        }
        used = BooleanArray(newCapacity)
        timeLevels = LongArray(newCapacity)
        longitudes = LongArray(newCapacity)
        latitudes = LongArray(newCapacity)
        oldUsed.indices.forEach { index ->
            if (oldUsed[index]) {
                val hash = hash(oldTimeLevels[index], oldLongitudes[index], oldLatitudes[index])
                var slot = slotFor(hash, newCapacity)
                while (used[slot]) slot = (slot + 1) % newCapacity
                used[slot] = true
                timeLevels[slot] = oldTimeLevels[index]
                longitudes[slot] = oldLongitudes[index]
                latitudes[slot] = oldLatitudes[index]
            }
        }
    }

    private fun slotFor(hash: Long, capacity: Int): Int = Math.floorMod(hash, capacity.toLong()).toInt()

    private fun capacityFor(required: Int): Int {
        if (required <= 0) return INITIAL_CAPACITY
        val scaled = checkedProduct(required.toLong(), LOAD_FACTOR_DENOMINATOR.toLong())
            .let { (it / LOAD_FACTOR_NUMERATOR) + 1L }
        return scaled.coerceIn(INITIAL_CAPACITY.toLong(), Int.MAX_VALUE.toLong()).toInt()
    }

    private fun requireCapacityWithinBudget(capacity: Int) {
        val bytes = checkedProduct(capacity.toLong(), BYTES_PER_SLOT)
        if (bytes > byteBudget) {
            throw NetCdfException.ResourceLimitExceeded("duplicate-coordinate-set", byteBudget, bytes)
        }
    }

    private companion object {
        const val INITIAL_CAPACITY: Int = 16
        const val LOAD_FACTOR_NUMERATOR: Int = 9
        const val LOAD_FACTOR_DENOMINATOR: Int = 10
        const val BYTES_PER_SLOT: Long = 25L

        fun canonicalDoubleBits(value: Double): Long =
            if (value == 0.0) 0L else java.lang.Double.doubleToLongBits(value)

        fun packTimeLevel(timeIdx: Int, levelIdx: Int): Long =
            (timeIdx.toLong() shl 32) xor (levelIdx.toLong() and 0xffffffffL)

        fun hash(timeLevel: Long, longitudeBits: Long, latitudeBits: Long): Long {
            var result = longitudeBits * -0x61c8864680b583ebL
            result = (result xor (result ushr 33)) * -0x3d4d51cb2c7b5a7dL
            result = (result xor latitudeBits) * -0x61c8864680b583ebL
            result = (result xor timeLevel) * -0x3d4d51cb2c7b5a7dL
            return result xor (result ushr 32)
        }
    }
}
