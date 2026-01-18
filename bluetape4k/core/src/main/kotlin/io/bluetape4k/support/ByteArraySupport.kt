package io.bluetape4k.support

import java.nio.ByteBuffer
import java.util.*


/**
 * [ByteArray]의 [count] 수만큼 앞에서부터 가져옵니다.
 *
 * ```
 * val array = byteArrayOf(1, 2, 3, 4, 5)
 * array.takeItems(3) // [1, 2, 3]
 * array.takeItems(0) // []
 * array.takeItems(5) // [1, 2, 3, 4, 5]
 * array.takeItems(10) // [1, 2, 3, 4, 5]
 * ```
 *
 * @param count 가져올 갯수 (0 < count <= size)
 */
fun ByteArray.takeItems(count: Int): ByteArray = when {
    count <= 0 -> emptyByteArray
    else -> this.copyOfRange(0, count.coerceAtMost(size))
}

/**
 * [ByteArray]의 [count] 수만큼 뒤에서부터 가져옵니다.
 *
 * ```
 * val array = byteArrayOf(1, 2, 3, 4, 5)
 * val result = array.dropItems(3) // [4, 5]
 * array.dropItems(0) // [1, 2, 3, 4, 5]
 * array.dropItems(5) // []
 * array.dropItems(10) // []
 * ```
 *
 * @param count drop 할 갯수 (0 < count <= size)
 */
fun ByteArray.dropItems(count: Int): ByteArray = when {
    count <= 0 -> this.copyOf()
    count >= size -> emptyByteArray
    else -> this.copyOfRange(count, size)
}

/**
 * Int 값을 ByteArray로 변환합니다.
 */
fun Int.toByteArray(): ByteArray {
    return ByteBuffer.allocate(4).putInt(this).array()
}

/**
 * Long 값을 ByteArray로 변환합니다.
 */
fun Long.toByteArray(): ByteArray {
    return ByteBuffer.allocate(8).putLong(this).array()
}

/**
 * [UUID]를 ByteArray로 변환합니다.
 *
 * ```
 * val uuid = UUID.randomUUID() // uuid=24738134-9d88-6645-4ec8-d63aa2031015
 * val bytes = uuid.toByteArray() // bytes=[36, 115, -123, 52, -99, -120, 102, 69, 78, -56, -42, 58, -94, 3, 16, 21]
 * ```
 */
fun UUID.toByteArray(): ByteArray {
    return ByteBuffer
        .allocate(16)
        .putLong(this.mostSignificantBits)
        .putLong(this.leastSignificantBits)
        .array()
}

/**
 * ByteArray의 값을 Int로 변환합니다.
 *
 * ```
 * val bytes = byteArrayOf(0, 0, 0, 1)
 * val result = bytes.toInt() // 1
 * ```
 */
fun ByteArray.toInt(offset: Int = 0): Int = ByteBuffer.wrap(this, offset, Int.SIZE_BYTES).int

/**
 * ByteArray의 값을 Long 으로 변환합니다.
 *
 * ```
 * val bytes = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1)
 * val result = bytes.toLong() // 1
 * ```
 */
fun ByteArray.toLong(offset: Int = 0): Long = ByteBuffer.wrap(this, offset, Long.SIZE_BYTES).long

/**
 * ByteArray의 값을 [UUID]로 변환합니다.
 *
 * ```
 * val bytes = byteArrayOf(36, 115, -123, 52, -99, -120, 102, 69, 78, -56, -42, 58, -94, 3, 16, 21)
 * val result = bytes.toUuid() // 24738134-9d88-6645-4ec8-d63aa2031015
 * ```
 */
fun ByteArray.toUuid(offset: Int = 0): UUID {
    val buffer = ByteBuffer.wrap(this, offset, 16)
    return UUID(buffer.long, buffer.long)
}

/**
 * ByteArray에 [target]값과 같은 첫번째 위치를 찾아 반환합니다.
 *
 * ```
 * val array = byteArrayOf(1, 2, 3, 4, 5)
 * val result = array.indexOf(3) // 2
 * ```
 *
 * @param target 찾을 Byte 값
 * @param start  시작 위치
 * @param end    끝 위치
 */
fun ByteArray.indexOf(target: Byte, start: Int = 0, end: Int = this.size): Int {
    start.requireZeroOrPositiveNumber("start")
    end.requireInRange(start, size, "end")

    for (i in start until end) {
        if (this[i] == target) {
            return i
        }
    }
    return -1
}

/**
 * ByteArray에 [target]값과 같은 첫번째 위치를 찾아 반환합니다.
 *
 * ```
 * val array = byteArrayOf(1, 2, 3, 4, 5)
 * val result = array.indexOf(byteArrayOf(3, 4)) // 2
 * ```
 *
 * @param target 찾을 ByteArray 값
 * @param start  시작 위치
 * @param end    끝 위치
 */
fun ByteArray.indexOf(target: ByteArray, start: Int = 0, end: Int = this.size): Int {
    start.requireZeroOrPositiveNumber("start")
    end.requireInRange(start, size, "end")

    if (target.isEmpty()) {
        return 0
    }

    outer@ for (i in start..<(end - target.size + 1)) {
        for (j in target.indices) {
            if (get(i + j) != target[j]) {
                continue@outer
            }
        }
        return i
    }
    return -1
}

/**
 * ByteArray에 [target]값과 같은 마지막 위치를 찾아 반환합니다.
 *
 * ```
 * val array = byteArrayOf(1, 2, 3, 4, 3)
 * val result = array.lastIndexOf(3) // 4
 * ```
 *
 * @param target 찾을 Byte 값
 * @param start  시작 위치
 * @param end    끝 위치
 */
fun ByteArray.lastIndexOf(target: Byte, start: Int, end: Int): Int {
    start.requireZeroOrPositiveNumber("start")
    end.requireInRange(start, size, "end")

    for (i in end - 1 downTo start) {
        if (this[i] == target) {
            return i
        }
    }
    return -1
}

/**
 * [ByteArray]의 크기를 늘려야 하는지 확인하고, 늘려야 한다면 [padding] 만큼 더 늘린 새로운 [ByteArray]를 반환합니다.
 *
 * ```
 * val array = byteArrayOf(1, 2, 3)
 * val result = array.ensureCapacity(5, 5) // [1, 2, 3, 0, 0, 0, 0, 0]
 * ```
 *
 * @param minCapacity 최소 크기
 * @param padding     늘릴 크기
 * @return 새로게 생성된 [ByteArray] (기존 Array 의 값은 복사된다)
 */
fun ByteArray.ensureCapacity(minCapacity: Int, padding: Int): ByteArray {
    minCapacity.requireZeroOrPositiveNumber("minCapacity")
    padding.requireZeroOrPositiveNumber("padding")

    val self = this@ensureCapacity
    if (self.size >= minCapacity) {
        return self
    }

    return ByteArray(minCapacity + padding).apply {
        self.copyInto(this, 0)
    }
}

/**
 * [arrays]를 합쳐서 하나의 [ByteArray]로 만듭니다.
 *
 * ```
 * val array1 = byteArrayOf(1, 2, 3)
 * val array2 = byteArrayOf(4, 5, 6)
 * val result = concat(array1, array2) // [1, 2, 3, 4, 5, 6]
 * ```
 *
 * @param arrays 합칠 ByteArray 배열
 * @return 합쳐진 ByteArray
 */
fun concat(vararg arrays: ByteArray): ByteArray {
    val totalSize = arrays.sumOf { it.size }
    val result = ByteArray(totalSize)
    var offset = 0
    for (array in arrays) {
        // System.arraycopy(array, 0, result, offset, array.size)
        array.copyInto(result, offset)
        offset += array.size
    }
    return result
}

/**
 * [ByteArray] 값을 역순으로 만든 새로운 [ByteArray]를 반환합니다.
 *
 * ```
 * val array = byteArrayOf(1, 2, 3, 4, 5)
 * val result = array.reverse() // [5, 4, 3, 2, 1]
 * ```
 *
 * @param fromIndex 시작 위치 (기본값: 0)
 * @param toIndex   끝 위치 (기본값: size-1)
 */
fun ByteArray.reverseTo(fromIndex: Int = 0, toIndex: Int = size - 1): ByteArray {
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInRange(fromIndex, size, "toIndex")

    val array = this@reverseTo
    return array.copyOf().apply {
        reverse(fromIndex, toIndex)
    }
}

/**
 * [ByteArray] 값을 역순으로 만듭니다.
 *
 * ```
 * val array = byteArrayOf(1, 2, 3, 4, 5)
 * array.reverse() // [5, 4, 3, 2, 1]
 * ```
 *
 * @param fromIndex 시작 위치 (기본값: 0)
 * @param toIndex   끝 위치 (기본값: size-1)
 */
fun ByteArray.reverse(fromIndex: Int = 0, toIndex: Int = size - 1) {
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInOpenRange(fromIndex, size, "toIndex")

    val array = this@reverse
    var i = fromIndex
    var j = toIndex
    while (i < j) {
        val tmp = array[i]
        array[i] = array[j]
        array[j] = tmp
        i++
        j--
    }
}

/**
 * [ByteArray]를 [distance] 만큼 회전시킨 새로운 [ByteArray]를 반환합니다.
 *
 * ```
 * val array = byteArrayOf(1, 2, 3, 4, 5)
 * val result = array.rotateTo(2) // [3, 4, 5, 1, 2]
 * val rotate2 = array.rotateTo(-2) // [4, 5, 1, 2, 3]
 * ```
 *
 * @param distance 회전할 거리
 * @param fromIndex 시작 위치 (기본값: 0)
 * @param toIndex 끝 위치 (기본값: size-1)
 * @return 회전된 ByteArray
 */
fun ByteArray.rotateTo(distance: Int, fromIndex: Int = 0, toIndex: Int = size - 1): ByteArray {
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInOpenRange(fromIndex, size, "toIndex")

    val array = this@rotateTo
    return array.copyOf().apply {
        rotate(distance, fromIndex, toIndex)
    }
}

/**
 * [ByteArray]를 [distance] 만큼 회전시킵니다.
 *
 * ```
 * val array = byteArrayOf(1, 2, 3, 4, 5)
 * array.rotate(2) // [3, 4, 5, 1, 2]
 * ```
 *
 * @param distance 회전할 거리
 * @param fromIndex 시작 위치 (기본값: 0)
 * @param toIndex 끝 위치 (기본값: size -1 )
 */
fun ByteArray.rotate(distance: Int, fromIndex: Int = 0, toIndex: Int = size - 1) {
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInOpenRange(fromIndex, size, "toIndex")

    val array = this@rotate

    if (array.size <= 1) {
        return
    }

    val length = toIndex - fromIndex + 1
    // obtain m = (-distance mod length), a non-negative value less than "length".
    // This is how many places left to rotate.
    var m = -distance % length
    m = if (m < 0) m + length else m

    // The current index of what will become the first element of the rotated section.
    val newFirstIndex = m + fromIndex
    if (newFirstIndex == fromIndex) {
        return
    }

    array.reverse(fromIndex, newFirstIndex - 1)
    array.reverse(newFirstIndex, toIndex)
    array.reverse(fromIndex, toIndex)
}
