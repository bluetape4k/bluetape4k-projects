package io.bluetape4k.math.commons


fun Double.isSpecialCase(): Boolean = this.isNaN() || this.isInfinite()

fun Double.isPositiveInfinite(): Boolean = this == Double.POSITIVE_INFINITY

fun Double.isNegativeInfinite(): Boolean = this == Double.NEGATIVE_INFINITY

fun Double.isMaxValue(): Boolean = this == Double.MAX_VALUE

fun Double.isMinValue(): Boolean = this == Double.MIN_VALUE

fun Float.isSpecialCase(): Boolean = this.isNaN() || this.isInfinite()

fun Float.isPositiveInfinite(): Boolean = this == Float.POSITIVE_INFINITY

fun Float.isNegativeInfinite(): Boolean = this == Float.NEGATIVE_INFINITY

fun Float.isMaxValue(): Boolean = this == Float.MAX_VALUE

fun Float.isMinValue(): Boolean = this == Float.MIN_VALUE
