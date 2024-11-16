package io.bluetape4k.support

/**
 * [IntArray]에서 [target]의 첫 번째 인덱스를 반환합니다.
 *
 * ```
 * val array = intArrayOf(1, 2, 3)
 * val index = array.indexOf(2)
 * ```
 *
 * @param target 찾을 값
 * @param start 시작 인덱스 (0부터 시작)
 * @param end 종료 인덱스 (size-1)
 * @return 찾은 값의 인덱스, 찾지 못한 경우 -1
 */
fun IntArray.indexOf(target: Int, start: Int = 0, end: Int = size - 1): Int {
    start.requireInOpenRange(0, end, "start")
    end.requireInOpenRange(start, size, "end")

    for (i in start until end) {
        if (this[i] == target) {
            return i
        }
    }
    return -1
}

/**
 * [IntArray]에서 [target]의 첫 번째 인덱스를 반환합니다.
 *
 * ```
 * val array = intArrayOf(1, 2, 3)
 * val index = array.indexOf(2)  // 1
 * ```
 *
 * @param target 찾을 값
 * @param start 시작 인덱스 (0부터 시작)
 * @param end 종료 인덱스 (size-1)
 * @return 찾은 값의 인덱스, 찾지 못한 경우 -1
 *
 */
fun IntArray.indexOf(target: IntArray, start: Int = 0, end: Int = this.size - 1): Int {
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
 * [IntArray]에서 [target]의 마지막 인덱스를 반환합니다.
 *
 * ```
 * val array = intArrayOf(1, 2, 3, 2)
 * val index = array.lastIndexOf(2)
 * ```
 *
 * @param target 찾을 값
 * @return 찾은 값의 인덱스, 찾지 못한 경우 -1
 */
fun IntArray.lastIndexOf(target: Int, start: Int = 0, end: Int = this@lastIndexOf.size): Int {
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
 * [IntArray]의 크기가 [minCapacity]보다 작으면 [padding]만큼 더 확장한 새로운 [IntArray]를 반환합니다.
 *
 * ```
 * val array = intArrayOf(1, 2, 3)
 * val newArray = array.ensureCapacity(5, 2)
 * println(newArray.contentToString()) // [1, 2, 3, 0, 0]
 * ```
 *
 * @param minCapacity 최소 크기
 * @param padding 추가할 크기
 * @return 확장된 [IntArray]
 */
fun IntArray.ensureCapacity(minCapacity: Int, padding: Int): IntArray {
    minCapacity.requireZeroOrPositiveNumber("minCapacity")
    padding.requireZeroOrPositiveNumber("padding")

    val self = this@ensureCapacity
    if (self.size >= minCapacity) {
        return self
    }

    val newCapacity = minCapacity + padding
    return IntArray(newCapacity).apply {
        self.copyInto(this, 0)
    }
}

/**
 * [arrays] 들을 모두 결합하여 새로운 [IntArray]를 반환합니다.
 *
 * ```
 * val array1 = intArrayOf(1, 2)
 * val array2 = intArrayOf(3, 4)
 * concat(array1, array2) // [1, 2, 3, 4]
 * ```
 *
 * @param arrays 결합할 [IntArray]들
 * @return 결합된 [IntArray]
 */
fun concat(vararg arrays: IntArray): IntArray {
    val totalLength = arrays.sumOf { it.size }
    val result = IntArray(totalLength)
    var offset = 0
    for (array in arrays) {
        array.copyInto(result, offset)
        offset += array.size
    }
    return result
}

/**
 * [IntArray]를 [fromIndex]부터 [toIndex]까지 역순으로 변경한 새로운 [IntArray]를 반환합니다.
 *
 * ```
 * val array = intArrayOf(1, 2, 3, 4)
 * val reversed = array.reverseTo(1, 3)
 * println(reversed.contentToString()) // [1, 4, 3, 2]
 * ```
 *
 * @param fromIndex 시작 인덱스 (0부터 시작)
 * @param toIndex 종료 인덱스 (size-1)
 * @return 역순으로 변경된 [IntArray]
 */
fun IntArray.reverseTo(fromIndex: Int = 0, toIndex: Int = size - 1): IntArray {
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInRange(fromIndex, size, "toIndex")

    return this@reverseTo.copyOf().apply {
        reverse(fromIndex, toIndex)
    }
}

/**
 * [IntArray]를 [fromIndex]부터 [toIndex]까지 역순으로 변경합니다.
 *
 * ```
 * val array = intArrayOf(1, 2, 3, 4)
 * array.reverse(1, 3)  // [1, 4, 3, 2]
 * ```
 *
 * @param fromIndex 시작 인덱스 (0부터 시작)
 * @param toIndex 종료 인덱스 (size-1)
 * @return 변경된 [IntArray]
 */
fun IntArray.reverse(fromIndex: Int = 0, toIndex: Int = size - 1) {
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInRange(fromIndex, size, "toIndex")

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
 * [IntArray]를 [distance]만큼 회전한 새로운 [IntArray]를 반환합니다.
 *
 * ```
 * val array = intArrayOf(1, 2, 3, 4)
 * val rotated = array.rotateTo(2)  // [3, 4, 1, 2]
 * val rotated2 = array.rotateTo(-1) // [2, 3, 4, 1]
 * ```
 *
 * @param distance 회전할 거리
 * @param fromIndex 시작 인덱스 (0부터 시작)
 * @param toIndex 종료 인덱스 (size-1)
 * @return 회전된 [IntArray]
 */
fun IntArray.rotateTo(distance: Int, fromIndex: Int = 0, toIndex: Int = size - 1): IntArray {
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInRange(fromIndex, size, "toIndex")

    return this@rotateTo.copyOf().apply {
        rotate(distance, fromIndex, toIndex)
    }
}

/**
 * [IntArray]를 [distance]만큼 회전합니다.
 *
 * ```
 * val array = intArrayOf(1, 2, 3, 4)
 * array.rotate(2)  // [3, 4, 1, 2]
 * array.rotate(-1) // [2, 3, 4, 1]
 * ```
 *
 * @param distance 회전할 거리
 * @param fromIndex 시작 인덱스 (0부터 시작)
 * @param toIndex 종료 인덱스 (size-1)
 * @return 회전된 [IntArray]
 */
fun IntArray.rotate(distance: Int, fromIndex: Int = 0, toIndex: Int = size - 1) {
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInOpenRange(fromIndex, size, "toIndex")

    val array = this@rotate

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

    array.reverse(fromIndex, newFirstIndex - 1)
    array.reverse(newFirstIndex, toIndex)
    array.reverse(fromIndex, toIndex)
}
