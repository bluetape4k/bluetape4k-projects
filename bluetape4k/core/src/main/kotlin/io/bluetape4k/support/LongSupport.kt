package io.bluetape4k.support

fun Long.toIntExact(): Int = try {
    Math.toIntExact(this)
} catch (e: ArithmeticException) {
    throw IllegalArgumentException("Value out of Int range. value=$this", e)
}
