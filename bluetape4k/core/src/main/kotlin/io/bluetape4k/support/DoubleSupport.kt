package io.bluetape4k.support

/**
 * Double 값이 유한한 값인지 판단합니다. [isInfinite] 이거나 [isNaN] 이면 유한한 값이 아닙니다.
 *
 * ```
 * 1.0.isFinite // true
 * Double.POSITIVE_INFINITY.isFinite // false
 * Double.NEGATIVE_INFINITY.isFinite // false
 * Double.NaN.isFinite // false
 * ```
 *
 */
val Double.isFinite: Boolean get() = !this.isInfinite() && !this.isNaN()

/**
 * Float 값이 유한한 값인지 판단합니다. [isInfinite] 이거나 [isNaN] 이면 유한한 값이 아닙니다.
 *
 * ```
 * 1.0F.isFinite // true
 * Float.POSITIVE_INFINITY.isFinite // false
 * Float.NEGATIVE_INFINITY.isFinite // false
 * Float.NaN.isFinite // false
 * ```
 *
 */
val Float.isFinite: Boolean get() = !this.isInfinite() && !this.isNaN()
