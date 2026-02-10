@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

inline fun <T: Number> floatArrayOf(vararg elements: T): FloatArray =
    FloatArray(elements.size) { elements[it].toFloat() }

/**
 * [FloatArray]에서 [target]을 범위 내에서 찾아서 첫 번째 인덱스를 반환합니다.
 *
 * ```
 * val array = floatArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * array.indexOf(3.0) // 2
 * ```
 *
 * @param target 찾을 Double 값
 * @param start  시작 위치 (기본값: 0)
 * @param end    끝 위치 (기본값: size-1)
 * @return 찾은 인덱스
 */
fun FloatArray.indexOf(target: Float, start: Int = 0, end: Int = this.size - 1): Int {
    if (this.isEmpty()) {
        return -1
    }
    start.requireInRange(0, end, "start")
    end.requireInOpenRange(start, size, "end")

    for (i in start..end) {
        if (this[i] == target) {
            return i
        }
    }
    return -1
}

/**
 * [FloatArray]에서 [target]을 범위 내에서 찾아서 첫 번째 인덱스를 반환합니다.
 *
 * ```
 * val array = floatArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * array.indexOf(floatArrayOf(3.0, 4.0)) // 2
 * ```
 *
 * @param target 찾을 FloatArray 값
 * @param start  시작 위치 (기본값: 0)
 * @param end    끝 위치 (기본값: size-1)
 * @return 찾은 인덱스
 */
fun FloatArray.indexOf(target: FloatArray, start: Int = 0, end: Int = this.size - 1): Int {
    if (this.isEmpty() || target.isEmpty()) {
        return -1
    }

    start.requireInRange(0, end, "start")
    end.requireInOpenRange(start, size, "end")

    outer@ for (i in start..(end - target.size)) {
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
 * [FloatArray]에서 [target]을 범위 내에서 찾아서 마지막 인덱스를 반환합니다.
 * 찾지 못한 경우 -1을 반환합니다.
 *
 * ```
 * val array = floatArrayOf(1.0, 2.0, 3.0, 4.0, 3.0)
 * array.lastIndexOf(3.0) // 4
 * ```
 *
 * @param target 찾을 Double 값
 * @param start  시작 위치 (기본값: 0)
 * @param end    끝 위치 (기본값: size-1)
 * @return 찾은 인덱스
 */
fun FloatArray.lastIndexOf(target: Float, start: Int = 0, end: Int = size - 1): Int {
    if (this.isEmpty()) {
        return -1
    }
    start.requireInRange(0, end, "start")
    end.requireInOpenRange(start, size, "end")

    this.min()
    for (i in end downTo start) {
        if (this[i] == target) {
            return i
        }
    }
    return -1
}

/**
 * [FloatArray]에서 [target]을 범위 내에서 찾아서 마지막 인덱스를 반환합니다.
 * 찾지 못한 경우 -1을 반환합니다.
 *
 * ```
 * val array = floatArrayOf(1, 2, 3, 4, 3)
 * array.lastIndexOf(floatArrayOf(3, 4)) // 2
 * ```
 *
 * @param target 찾을 FloatArray 값
 * @param start  시작 위치 (기본값: 0)
 * @param end    끝 위치 (기본값: size-1)
 * @return 찾은 인덱스
 */
fun FloatArray.lastIndexOf(target: FloatArray, start: Int = 0, end: Int = size - 1): Int {
    if (this.isEmpty() || target.isEmpty()) {
        return -1
    }

    start.requireInRange(0, end, "start")
    end.requireInOpenRange(start, size, "end")

    outer@ for (i in (end - target.size) downTo start) {
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
 * [FloatArray]의 사이즈가 [minCapacity]보다 작을 경우, [padding]만큼 더한 새로운 [FloatArray]를 반환합니다.
 *
 * ```
 * val array = floatArrayOf(1, 2, 3)
 * val result = array.ensureCapacity(5, 5) // [1.0, 2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0]
 * ```
 *
 * @param minCapacity 최소 크기
 * @param padding    패딩 크기
 * @return 새로운 FloatArray
 */
fun FloatArray.ensureCapacity(minCapacity: Int, padding: Int): FloatArray {
    minCapacity.requireZeroOrPositiveNumber("minCapacity")
    padding.requireZeroOrPositiveNumber("padding")

    val self = this@ensureCapacity
    if (self.size >= minCapacity) {
        return self
    }

    val newCapacity = minCapacity + padding
    return FloatArray(newCapacity).apply {
        self.copyInto(this, 0)
    }
}

/**
 * 여러 [FloatArray]를 합쳐서 하나의 [FloatArray]로 만듭니다.
 *
 * ```
 * val array1 = floatArrayOf(1.0, 2.0)
 * val array2 = floatArrayOf(3.0, 4.0)
 * val result = concat(array1, array2) // [1.0, 2.0, 3.0, 4.0]
 * ```
 *
 * @param arrays 합칠 FloatArray들
 * @return 합쳐진 FloatArray
 */
fun concat(vararg arrays: FloatArray): FloatArray {
    val totalLength = arrays.sumOf { it.size }
    val result = FloatArray(totalLength)
    var offset = 0
    for (array in arrays) {
        array.copyInto(result, offset)
        offset += array.size
    }
    return result
}

/**
 * [FloatArray]의 값을 역순으로 만든 새로운 [FloatArray]를 반환합니다.
 *
 * ```
 * val array = floatArrayOf(1, 2, 3, 4, 5)
 * val result = array.reverseTo() // [5.0, 4.0, 3.0, 2.0, 1.0]
 * ```
 *
 * @param fromIndex 시작 인덱스 (기본값: 0)
 * @param toIndex 끝 인덱스 (기본값: size-1)
 * @return 역순으로 만든 FloatArray
 *
 */
fun FloatArray.reverseTo(fromIndex: Int = 0, toIndex: Int = size - 1): FloatArray {
    if (isEmpty()) {
        return emptyFloatArray
    }
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInOpenRange(fromIndex, size, "toIndex")

    return copyOf().apply {
        reverseThis(fromIndex, toIndex)
    }
}

/**
 * [FloatArray]의 값을 역순으로 만듭니다.
 *
 * ```
 * val array = floatArrayOf(1, 2, 3, 4, 5)
 * 
 * array.reverseThis() // [5.0, 4.0, 3.0, 2.0, 1.0]
 * array.reverseThis(1, 4) // [1.0, 5.0, 4.0, 3.0, 2.0]
 * ```
 *
 * @param fromIndex 시작 인덱스 (기본값: 0)
 * @param toIndex 끝 인덱스 (기본값: size-1)
 */
fun FloatArray.reverseThis(fromIndex: Int = 0, toIndex: Int = size - 1) {
    if (isEmpty()) {
        return
    }
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInOpenRange(fromIndex, size, "toIndex")

    var i = fromIndex
    var j = toIndex
    while (i < j) {
        val temp = this[i]
        this[i] = this[j]
        this[j] = temp
        i++
        j--
    }
}

/**
 * [FloatArray]를 [distance]만큼 회전시킨 새로운 [FloatArray]를 반환합니다.
 *
 * ```
 * val array = floatArrayOf(1, 2, 3, 4, 5)
 * val result = array.rotateTo(2) // [3.0, 4.0, 5.0, 1.0, 2.0]
 * val result2 = array.rotateTo(-2) // [4.0, 5.0, 1.0, 2.0, 3.0]
 * ```
 *
 * @param distance 회전할 거리
 * @param fromIndex 시작 인덱스 (기본값: 0)
 * @param toIndex 끝 인덱스 (기본값: size-1)
 * @return 회전된 FloatArray
 */
fun FloatArray.rotateTo(distance: Int, fromIndex: Int = 0, toIndex: Int = size - 1): FloatArray {
    if (isEmpty()) {
        return emptyFloatArray
    }

    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInOpenRange(fromIndex, size, "toIndex")

    return copyOf().apply {
        rotateThis(distance, fromIndex, toIndex)
    }
}

/**
 * [FloatArray]를 [distance]만큼 회전시킵니다.
 *
 * ```
 * val array = floatArrayOf(1, 2, 3, 4, 5)
 * array.rotate(2)  // [3.0, 4.0, 5.0, 1.0, 2.0]
 * array.rotate(-2) // [4.0, 5.0, 1.0, 2.0, 3.0]
 * ```
 *
 * @param distance 회전할 거리
 * @param fromIndex 시작 인덱스 (기본값: 0)
 * @param toIndex 끝 인덱스 (기본값: size-1)
 */
fun FloatArray.rotateThis(distance: Int, fromIndex: Int = 0, toIndex: Int = size - 1) {
    if (isEmpty()) {
        return
    }
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInOpenRange(fromIndex, size, "toIndex")

    val array = this@rotateThis

    if (array.size <= 1) {
        return
    }

    val length = toIndex - fromIndex + 1
    // Obtain m = (-distance mod length), a non-negative value less than "length". This is how many
    // places left to rotate.
    var m = -distance % length
    m = if (m < 0) m + length else m

    // The current index of what will become the first element of the rotated section.
    val newFirstIndex = m + fromIndex
    if (newFirstIndex == fromIndex) {
        return
    }

    with(array) {
        reverseThis(fromIndex, newFirstIndex - 1)
        reverseThis(newFirstIndex, toIndex)
        reverseThis(fromIndex, toIndex)
    }
}
