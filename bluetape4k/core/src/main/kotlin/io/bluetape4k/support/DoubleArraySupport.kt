package io.bluetape4k.support

/**
 * [DoubleArray]에서 [target]을 범위 내에서 찾아서 첫 번째 인덱스를 반환합니다.
 *
 * ```
 * val array = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * array.indexOf(3.0) // 2
 * ```
 *
 * @param target 찾을 Double 값
 * @param start  시작 위치 (기본값: 0)
 * @param end    끝 위치 (기본값: size-1)
 * @return 찾은 인덱스
 */
fun DoubleArray.indexOf(target: Double, start: Int = 0, end: Int = this.size - 1): Int {
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
 * [DoubleArray]에서 [target]을 범위 내에서 찾아서 첫 번째 인덱스를 반환합니다.
 *
 * ```
 * val array = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * array.indexOf(doubleArrayOf(3.0, 4.0)) // 2
 * ```
 *
 * @param target 찾을 DoubleArray 값
 * @param start  시작 위치 (기본값: 0)
 * @param end    끝 위치 (기본값: size-1)
 * @return 찾은 인덱스
 */
fun DoubleArray.indexOf(target: DoubleArray, start: Int = 0, end: Int = this.size - 1): Int {
    start.requireInOpenRange(0, end, "start")
    end.requireInOpenRange(start, size, "end")

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
 * [DoubleArray]에서 [target]을 범위 내에서 찾아서 마지막 인덱스를 반환합니다.
 * 찾지 못한 경우 -1을 반환합니다.
 *
 * ```
 * val array = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 3.0)
 * array.lastIndexOf(3.0) // 4
 * ```
 *
 * @param target 찾을 Double 값
 * @param start  시작 위치 (기본값: 0)
 * @param end    끝 위치 (기본값: size)
 * @return 찾은 인덱스
 */
fun DoubleArray.lastIndexOf(target: Double, start: Int = 0, end: Int = this@lastIndexOf.size - 1): Int {
    start.requireZeroOrPositiveNumber("start")
    end.requireInOpenRange(start, size, "end")

    this.min()
    for (i in end - 1 downTo start) {
        if (this[i] == target) {
            return i
        }
    }
    return -1

}

/**
 * [DoubleArray]의 사이즈가 [minCapacity]보다 작을 경우, [padding]만큼 더한 새로운 [DoubleArray]를 반환합니다.
 *
 * ```
 * val array = doubleArrayOf(1.0, 2.0, 3.0)
 * val result = array.ensureCapacity(5, 5) // [1.0, 2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0]
 * ```
 *
 * @param minCapacity 최소 크기
 * @param padding    패딩 크기
 * @return 새로운 DoubleArray
 */
fun DoubleArray.ensureCapacity(minCapacity: Int, padding: Int): DoubleArray {
    minCapacity.requireZeroOrPositiveNumber("minCapacity")
    padding.requireZeroOrPositiveNumber("padding")

    val self = this@ensureCapacity
    if (self.size >= minCapacity) {
        return self
    }

    val newCapacity = minCapacity + padding
    return DoubleArray(newCapacity).apply {
        self.copyInto(this, 0)
    }
}

/**
 * 여러 [DoubleArray]를 합쳐서 하나의 [DoubleArray]로 만듭니다.
 *
 * ```
 * val array1 = doubleArrayOf(1.0, 2.0)
 * val array2 = doubleArrayOf(3.0, 4.0)
 * val result = concat(array1, array2) // [1.0, 2.0, 3.0, 4.0]
 * ```
 *
 * @param arrays 합칠 DoubleArray들
 * @return 합쳐진 DoubleArray
 */
fun concat(vararg arrays: DoubleArray): DoubleArray {
    val totalLength = arrays.sumOf { it.size }
    val result = DoubleArray(totalLength)
    var offset = 0
    for (array in arrays) {
        array.copyInto(result, offset)
        offset += array.size
    }
    return result
}

/**
 * [DoubleArray]의 값을 역순으로 만든 새로운 [DoubleArray]를 반환합니다.
 *
 * ```
 * val array = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = array.reverseTo() // [5.0, 4.0, 3.0, 2.0, 1.0]
 * ```
 *
 * @param fromIndex 시작 인덱스 (기본값: 0)
 * @param toIndex 끝 인덱스 (기본값: size)
 * @return 역순으로 만든 DoubleArray
 *
 */
fun DoubleArray.reverseTo(fromIndex: Int = 0, toIndex: Int = size - 1): DoubleArray {
    fromIndex.requireInRange(0, size - 1, "fromIndex")
    toIndex.requireInOpenRange(fromIndex, size, "toIndex")

    return this@reverseTo.copyOf().apply {
        reverse(fromIndex, toIndex)
    }
}

/**
 * [DoubleArray]의 값을 역순으로 만듭니다.
 *
 * ```
 * val array = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * array.reverse() // [5.0, 4.0, 3.0, 2.0, 1.0]
 * ```
 *
 * @param fromIndex 시작 인덱스 (기본값: 0)
 * @param toIndex 끝 인덱스 (기본값: size)
 */
fun DoubleArray.reverse(fromIndex: Int = 0, toIndex: Int = size - 1) {
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
 * [DoubleArray]를 [distance]만큼 회전시킨 새로운 [DoubleArray]를 반환합니다.
 *
 * ```
 * val array = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * val result = array.rotateTo(2) // [3.0, 4.0, 5.0, 1.0, 2.0]
 * val result2 = array.rotateTo(-2) // [4.0, 5.0, 1.0, 2.0, 3.0]
 * ```
 *
 * @param distance 회전할 거리
 * @param fromIndex 시작 인덱스 (기본값: 0)
 * @param toIndex 끝 인덱스 (기본값: size)
 * @return 회전된 DoubleArray
 */
fun DoubleArray.rotateTo(distance: Int, fromIndex: Int = 0, toIndex: Int = size - 1): DoubleArray {
    fromIndex.requireInRange(0, toIndex, "fromIndex")
    toIndex.requireInOpenRange(fromIndex, size, "toIndex")

    return this@rotateTo.copyOf().apply {
        rotate(distance, fromIndex, toIndex)
    }
}

/**
 * [DoubleArray]를 [distance]만큼 회전시킵니다.
 *
 * ```
 * val array = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0)
 * array.rotate(2)  // [3.0, 4.0, 5.0, 1.0, 2.0]
 * array.rotate(-2) // [4.0, 5.0, 1.0, 2.0, 3.0]
 * ```
 *
 * @param distance 회전할 거리
 * @param fromIndex 시작 인덱스 (기본값: 0)
 * @param toIndex 끝 인덱스 (기본값: size - 1)
 */
fun DoubleArray.rotate(distance: Int, fromIndex: Int = 0, toIndex: Int = size - 1) {
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
