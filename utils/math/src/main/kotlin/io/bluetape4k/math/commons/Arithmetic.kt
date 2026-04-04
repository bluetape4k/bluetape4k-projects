package io.bluetape4k.math.commons

/**
 * 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 2.0).plus(sequenceOf(10.0, 20.0)).toList()
 * // [11.0, 22.0]
 * ```
 *
 * @param right right side collection
 * @return 요소별 덧셈 결과 시퀀스
 */
@JvmName("plusOfDoubleSequence")
operator fun Sequence<Double>.plus(right: Sequence<Double>): Sequence<Double> =
    this.zip(right).map { (lhs, rhs) -> lhs + rhs }

/**
 * 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(1.0, 2.0).plus(listOf(10.0, 20.0)).toList()
 * // [11.0, 22.0]
 * ```
 */
@JvmName("plusOfDoubleIterable")
operator fun Iterable<Double>.plus(right: Iterable<Double>): Iterable<Double> =
    asSequence().plus(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(10.0, 20.0).minus(sequenceOf(1.0, 2.0)).toList()
 * // [9.0, 18.0]
 * ```
 *
 * @param right right side collection
 * @return 요소별 뺄셈 결과 시퀀스
 */
@JvmName("minusOfDoubleSequence")
operator fun Sequence<Double>.minus(right: Sequence<Double>): Sequence<Double> =
    this.zip(right).map { (lhs, rhs) -> lhs - rhs }

/**
 * 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(10.0, 20.0).minus(listOf(1.0, 2.0)).toList()
 * // [9.0, 18.0]
 * ```
 */
@JvmName("minusOfDoubleIterable")
operator fun Iterable<Double>.minus(right: Iterable<Double>): Iterable<Double> =
    asSequence().minus(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(2.0, 3.0).times(sequenceOf(4.0, 5.0)).toList()
 * // [8.0, 15.0]
 * ```
 *
 * @param right right side collection
 * @return 요소별 곱셈 결과 시퀀스
 */
@JvmName("timesOfDoubleSequence")
operator fun Sequence<Double>.times(right: Sequence<Double>): Sequence<Double> =
    this.zip(right).map { (lhs, rhs) -> lhs * rhs }

/**
 * 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(2.0, 3.0).times(listOf(4.0, 5.0)).toList()
 * // [8.0, 15.0]
 * ```
 */
@JvmName("timesOfDoubleIterable")
operator fun Iterable<Double>.times(right: Iterable<Double>): Iterable<Double> =
    asSequence().times(right.asSequence()).asIterable()

/**
 * 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(10.0, 20.0).div(sequenceOf(2.0, 4.0)).toList()
 * // [5.0, 5.0]
 * ```
 *
 * @param right right side collection
 * @return 요소별 나눗셈 결과 시퀀스
 */
@JvmName("divOfDoubleSequence")
operator fun Sequence<Double>.div(right: Sequence<Double>): Sequence<Double> =
    this.zip(right).map { (lhs, rhs) -> lhs / rhs }

/**
 * 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(10.0, 20.0).div(listOf(2.0, 4.0)).toList()
 * // [5.0, 5.0]
 * ```
 */
@JvmName("divOfDoubleIterable")
operator fun Iterable<Double>.div(right: Iterable<Double>): Iterable<Double> =
    asSequence().div(right.asSequence()).asIterable()

/**
 * Float 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0f, 2.0f).plus(sequenceOf(10.0f, 20.0f)).toList()
 * // [11.0, 22.0]
 * ```
 *
 * @param right right side collection
 * @return 요소별 덧셈 결과 시퀀스
 */
@JvmName("plusOfFloatSequence")
operator fun Sequence<Float>.plus(right: Sequence<Float>): Sequence<Float> =
    this.zip(right).map { (lhs, rhs) -> lhs + rhs }

/**
 * Float 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(1.0f, 2.0f).plus(listOf(10.0f, 20.0f)).toList()
 * // [11.0, 22.0]
 * ```
 */
@JvmName("plusOfFloatIterable")
operator fun Iterable<Float>.plus(right: Iterable<Float>): Iterable<Float> =
    asSequence().plus(right.asSequence()).asIterable()

/**
 * Float 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(10.0f, 20.0f).minus(sequenceOf(1.0f, 2.0f)).toList()
 * // [9.0, 18.0]
 * ```
 *
 * @param right right side collection
 * @return 요소별 뺄셈 결과 시퀀스
 */
@JvmName("minusOfFloatSequence")
operator fun Sequence<Float>.minus(right: Sequence<Float>): Sequence<Float> =
    this.zip(right).map { (lhs, rhs) -> lhs - rhs }

/**
 * Float 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(10.0f, 20.0f).minus(listOf(1.0f, 2.0f)).toList()
 * // [9.0, 18.0]
 * ```
 */
@JvmName("minusOfFloatIterable")
operator fun Iterable<Float>.minus(right: Iterable<Float>): Iterable<Float> =
    asSequence().minus(right.asSequence()).asIterable()

/**
 * Float 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(2.0f, 3.0f).times(sequenceOf(4.0f, 5.0f)).toList()
 * // [8.0, 15.0]
 * ```
 *
 * @param right right side collection
 * @return 요소별 곱셈 결과 시퀀스
 */
@JvmName("timesOfFloatSequence")
operator fun Sequence<Float>.times(right: Sequence<Float>): Sequence<Float> =
    this.zip(right).map { (lhs, rhs) -> lhs * rhs }

/**
 * Float 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(2.0f, 3.0f).times(listOf(4.0f, 5.0f)).toList()
 * // [8.0, 15.0]
 * ```
 */
@JvmName("timesOfFloatIterable")
operator fun Iterable<Float>.times(right: Iterable<Float>): Iterable<Float> =
    asSequence().times(right.asSequence()).asIterable()


/**
 * Float 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(10.0f, 20.0f).div(sequenceOf(2.0f, 4.0f)).toList()
 * // [5.0, 5.0]
 * ```
 *
 * @param right right side collection
 * @return 요소별 나눗셈 결과 시퀀스
 */
@JvmName("divOfFloatSequence")
operator fun Sequence<Float>.div(right: Sequence<Float>): Sequence<Float> =
    this.zip(right).map { (lhs, rhs) -> lhs / rhs }

/**
 * Float 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(10.0f, 20.0f).div(listOf(2.0f, 4.0f)).toList()
 * // [5.0, 5.0]
 * ```
 */
@JvmName("divOfFloatIterable")
operator fun Iterable<Float>.div(right: Iterable<Float>): Iterable<Float> =
    asSequence().div(right.asSequence()).asIterable()


/**
 * Long 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1L, 2L).plus(sequenceOf(10L, 20L)).toList()
 * // [11, 22]
 * ```
 *
 * @param right right side collection
 * @return 요소별 덧셈 결과 시퀀스
 */
@JvmName("plusOfLongSequence")
operator fun Sequence<Long>.plus(right: Sequence<Long>): Sequence<Long> =
    this.zip(right).map { (lhs, rhs) -> lhs + rhs }

/**
 * Long 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(1L, 2L).plus(listOf(10L, 20L)).toList()
 * // [11, 22]
 * ```
 */
@JvmName("plusOfLongIterable")
operator fun Iterable<Long>.plus(right: Iterable<Long>): Iterable<Long> =
    asSequence().plus(right.asSequence()).asIterable()


/**
 * Long 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(10L, 20L).minus(sequenceOf(1L, 2L)).toList()
 * // [9, 18]
 * ```
 *
 * @param right right side collection
 * @return 요소별 뺄셈 결과 시퀀스
 */
@JvmName("minusOfLongSequence")
operator fun Sequence<Long>.minus(right: Sequence<Long>): Sequence<Long> =
    this.zip(right).map { (lhs, rhs) -> lhs - rhs }

/**
 * Long 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(10L, 20L).minus(listOf(1L, 2L)).toList()
 * // [9, 18]
 * ```
 */
@JvmName("minusOfLongIterable")
operator fun Iterable<Long>.minus(right: Iterable<Long>): Iterable<Long> =
    asSequence().minus(right.asSequence()).asIterable()

/**
 * Long 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(2L, 3L).times(sequenceOf(4L, 5L)).toList()
 * // [8, 15]
 * ```
 *
 * @param right right side collection
 * @return 요소별 곱셈 결과 시퀀스
 */
@JvmName("timesOfLongSequence")
operator fun Sequence<Long>.times(right: Sequence<Long>): Sequence<Long> =
    this.zip(right).map { (lhs, rhs) -> lhs * rhs }

/**
 * Long 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(2L, 3L).times(listOf(4L, 5L)).toList()
 * // [8, 15]
 * ```
 */
@JvmName("timesOfLongIterable")
operator fun Iterable<Long>.times(right: Iterable<Long>): Iterable<Long> =
    asSequence().times(right.asSequence()).asIterable()

/**
 * Long 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(10L, 20L).div(sequenceOf(2L, 4L)).toList()
 * // [5, 5]
 * ```
 *
 * @param right right side collection
 * @return 요소별 나눗셈 결과 시퀀스
 */
@JvmName("divOfLongSequence")
operator fun Sequence<Long>.div(right: Sequence<Long>): Sequence<Long> =
    this.zip(right).map { (lhs, rhs) -> lhs / rhs }

/**
 * Long 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(10L, 20L).div(listOf(2L, 4L)).toList()
 * // [5, 5]
 * ```
 */
@JvmName("divOfLongIterable")
operator fun Iterable<Long>.div(right: Iterable<Long>): Iterable<Long> =
    asSequence().div(right.asSequence()).asIterable()

/**
 * Int 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1, 2).plus(sequenceOf(10, 20)).toList()
 * // [11, 22]
 * ```
 *
 * @param right right side collection
 * @return 요소별 덧셈 결과 시퀀스
 */
@JvmName("plusOfIntSequence")
operator fun Sequence<Int>.plus(right: Sequence<Int>): Sequence<Int> =
    this.zip(right).map { (lhs, rhs) -> lhs + rhs }

/**
 * Int 컬렉션의 각 요소들을 Plus를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(1, 2).plus(listOf(10, 20)).toList()
 * // [11, 22]
 * ```
 */
@JvmName("plusOfIntIterable")
operator fun Iterable<Int>.plus(right: Iterable<Int>): Iterable<Int> =
    asSequence().plus(right.asSequence()).asIterable()

/**
 * Int 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(10, 20).minus(sequenceOf(1, 2)).toList()
 * // [9, 18]
 * ```
 *
 * @param right right side collection
 * @return 요소별 뺄셈 결과 시퀀스
 */
@JvmName("minusOfIntSequence")
operator fun Sequence<Int>.minus(right: Sequence<Int>): Sequence<Int> =
    this.zip(right).map { (lhs, rhs) -> lhs - rhs }

/**
 * Int 컬렉션의 각 요소들을 Minus를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(10, 20).minus(listOf(1, 2)).toList()
 * // [9, 18]
 * ```
 */
@JvmName("minusOfIntIterable")
operator fun Iterable<Int>.minus(right: Iterable<Int>): Iterable<Int> =
    asSequence().minus(right.asSequence()).asIterable()

/**
 * Int 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(2, 3).times(sequenceOf(4, 5)).toList()
 * // [8, 15]
 * ```
 *
 * @param right right side collection
 * @return 요소별 곱셈 결과 시퀀스
 */
@JvmName("timesOfIntSequence")
operator fun Sequence<Int>.times(right: Sequence<Int>): Sequence<Int> =
    this.zip(right).map { (lhs, rhs) -> lhs * rhs }

/**
 * Int 컬렉션의 각 요소들을 Multiply를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(2, 3).times(listOf(4, 5)).toList()
 * // [8, 15]
 * ```
 */
@JvmName("timesOfIntIterable")
operator fun Iterable<Int>.times(right: Iterable<Int>): Iterable<Int> =
    asSequence().times(right.asSequence()).asIterable()

/**
 * Int 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * ```kotlin
 * val result = sequenceOf(10, 20).div(sequenceOf(2, 4)).toList()
 * // [5, 5]
 * ```
 *
 * @param right right side collection
 * @return 요소별 나눗셈 결과 시퀀스
 */
@JvmName("divOfIntSequence")
operator fun Sequence<Int>.div(right: Sequence<Int>): Sequence<Int> =
    this.zip(right).map { (lhs, rhs) -> lhs / rhs }

/**
 * Int 컬렉션의 각 요소들을 Div를 수행합니다.
 *
 * ```kotlin
 * val result = listOf(10, 20).div(listOf(2, 4)).toList()
 * // [5, 5]
 * ```
 */
@JvmName("divOfIntIterable")
operator fun Iterable<Int>.div(right: Iterable<Int>): Iterable<Int> =
    asSequence().div(right.asSequence()).asIterable()
