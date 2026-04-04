package io.bluetape4k.math.commons

/**
 * Double이 NaN 또는 Infinite인 특수값인지 검사합니다.
 *
 * ```kotlin
 * val result = Double.NaN.isSpecialCase()        // true
 * val result2 = Double.POSITIVE_INFINITY.isSpecialCase()  // true
 * val result3 = 1.0.isSpecialCase()              // false
 * ```
 */
fun Double.isSpecialCase(): Boolean = this.isNaN() || this.isInfinite()

/**
 * Double이 양의 무한대인지 검사합니다.
 *
 * ```kotlin
 * val result = Double.POSITIVE_INFINITY.isPositiveInfinite()   // true
 * val result2 = 1.0.isPositiveInfinite()                       // false
 * ```
 */
fun Double.isPositiveInfinite(): Boolean = this == Double.POSITIVE_INFINITY

/**
 * Double이 음의 무한대인지 검사합니다.
 *
 * ```kotlin
 * val result = Double.NEGATIVE_INFINITY.isNegativeInfinite()   // true
 * val result2 = 1.0.isNegativeInfinite()                       // false
 * ```
 */
fun Double.isNegativeInfinite(): Boolean = this == Double.NEGATIVE_INFINITY

/**
 * Double이 Double.MAX_VALUE와 같은지 검사합니다.
 *
 * ```kotlin
 * val result = Double.MAX_VALUE.isMaxValue()   // true
 * val result2 = 1.0.isMaxValue()               // false
 * ```
 */
fun Double.isMaxValue(): Boolean = this == Double.MAX_VALUE

/**
 * Double이 Double.MIN_VALUE와 같은지 검사합니다.
 *
 * ```kotlin
 * val result = Double.MIN_VALUE.isMinValue()   // true
 * val result2 = 1.0.isMinValue()               // false
 * ```
 */
fun Double.isMinValue(): Boolean = this == Double.MIN_VALUE

/**
 * Float이 NaN 또는 Infinite인 특수값인지 검사합니다.
 *
 * ```kotlin
 * val result = Float.NaN.isSpecialCase()       // true
 * val result2 = 1.0f.isSpecialCase()           // false
 * ```
 */
fun Float.isSpecialCase(): Boolean = this.isNaN() || this.isInfinite()

/**
 * Float이 양의 무한대인지 검사합니다.
 *
 * ```kotlin
 * val result = Float.POSITIVE_INFINITY.isPositiveInfinite()   // true
 * val result2 = 1.0f.isPositiveInfinite()                     // false
 * ```
 */
fun Float.isPositiveInfinite(): Boolean = this == Float.POSITIVE_INFINITY

/**
 * Float이 음의 무한대인지 검사합니다.
 *
 * ```kotlin
 * val result = Float.NEGATIVE_INFINITY.isNegativeInfinite()   // true
 * val result2 = 1.0f.isNegativeInfinite()                     // false
 * ```
 */
fun Float.isNegativeInfinite(): Boolean = this == Float.NEGATIVE_INFINITY

/**
 * Float이 Float.MAX_VALUE와 같은지 검사합니다.
 *
 * ```kotlin
 * val result = Float.MAX_VALUE.isMaxValue()   // true
 * val result2 = 1.0f.isMaxValue()             // false
 * ```
 */
fun Float.isMaxValue(): Boolean = this == Float.MAX_VALUE

/**
 * Float이 Float.MIN_VALUE와 같은지 검사합니다.
 *
 * ```kotlin
 * val result = Float.MIN_VALUE.isMinValue()   // true
 * val result2 = 1.0f.isMinValue()             // false
 * ```
 */
fun Float.isMinValue(): Boolean = this == Float.MIN_VALUE
