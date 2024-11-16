package io.bluetape4k.support

/**
 * Double 값이 유한한 값인지 판단합니다.
 *
 * ```
 * 1.0.isFinite // true
 * Double.POSITIVE_INFINITY.isFinite // false
 * Double.NEGATIVE_INFINITY.isFinite // false
 * Double.NaN.isFinite // false
 * ```
 *
 */
val Double.isFinite: Boolean get() = Double.NEGATIVE_INFINITY < this && this < Double.POSITIVE_INFINITY
