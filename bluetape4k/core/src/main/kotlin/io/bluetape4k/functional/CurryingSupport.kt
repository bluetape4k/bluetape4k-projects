package io.bluetape4k.functional


/**
 * 지정한 함수를 currying 함수로 변환합니다.
 *
 * ```kotlin
 *
 * val func : (Int, Int) -> Int = { a, b -> a + b }
 * val curried = func.currying()
 *
 * val result = curried(1)(2)  // returns 3
 *
 * val func1 = func.curried()(1)
 * val result = func1(3)       // returns 4
 * ```
 *
 * @receiver Function2<T1, T2, R>
 * @return (T1) -> (T2) -> R
 */
fun <T1, T2, R> ((T1, T2) -> R).curried(): (T1) -> (T2) -> R =
    { t1: T1 -> { t2: T2 -> this(t1, t2) } }

/**
 * 지정한 함수를 currying 함수로 변환합니다.
 *
 * ```kotlin
 *
 * val func : (Int, Int, Int) -> Int = { a, b, c -> a + b + c }
 * val curried = func.currying()
 *
 * val result = curried(1)(2)(3)  // returns 6
 *
 * val func2 = func.curried()(1)
 * val result = func2(3)(4)       // returns 1 + 3 + 4
 * ```
 *
 * @receiver Function3<T1, T2, T3, R>
 * @return (T1) -> (T2) -> (T3) -> R
 */
fun <T1, T2, T3, R> ((T1, T2, T3) -> R).curried(): (T1) -> (T2) -> (T3) -> R =
    { t1: T1 -> { t2: T2 -> { t3: T3 -> this(t1, t2, t3) } } }

/**
 * 지정한 함수를 currying 함수로 변환합니다.
 *
 * ```kotlin
 *
 * val func : (Int, Int, Int, Int) -> Int = { a, b, c, d -> a + b + c + d }
 * val curried = func.currying()
 *
 * val result = curried(1)(2)(3)(4)  // returns 10
 *
 * val func2 = func.curried()(1)
 * val result = func2(3)(4)(5)       // returns 1 + 3 + 4 + 5
 * ```
 *
 * @receiver Function4<T1, T2, T3, T4, R>
 * @return (T1) -> (T2) -> (T3) -> (T4) -> R
 */
fun <T1, T2, T3, T4, R> ((T1, T2, T3, T4) -> R).curried(): (T1) -> (T2) -> (T3) -> (T4) -> R =
    { t1: T1 -> { t2: T2 -> { t3: T3 -> { t4: T4 -> this(t1, t2, t3, t4) } } } }

/**
 * 지정한 함수를 currying 함수로 변환합니다.
 *
 * ```kotlin
 *
 * val func : (Int, Int, Int, Int) -> Int = { a, b, c, d -> a + b + c + d }
 * val curried = func.currying()
 *
 * val result = curried(1)(2)(3)(4)  // returns 10
 *
 * val func2 = func.curried()(1)
 * val result = func2(3)(4)(5)       // returns 1 + 3 + 4 + 5
 * ```
 *
 * @receiver Function5<T1, T2, T3, T4, T5, R>
 * @return (T1) -> (T2) -> (T3) -> (T4) -> (T5) -> R
 */
fun <T1, T2, T3, T4, T5, R> ((T1, T2, T3, T4, T5) -> R).curried(): (T1) -> (T2) -> (T3) -> (T4) -> (T5) -> R =
    { t1: T1 -> { t2: T2 -> { t3: T3 -> { t4: T4 -> { t5: T5 -> this(t1, t2, t3, t4, t5) } } } } }
