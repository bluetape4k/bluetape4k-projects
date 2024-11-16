package io.bluetape4k.support

/**
 * 값이 true 라면 [block]을 실행합니다. false라면 null을 반환합니다.
 *
 * ```
 * val result = true.ifTrue { "true" } ?: "false"  // "true"
 * val result = false.ifTrue { "true" } ?: "false" // "false"
 * ```
 */
inline fun <T: Any> Boolean.ifTrue(block: () -> T): T? = when (this) {
    true -> block()
    else -> null
}

/**
 * 함수가 true를 반환하면 [block]을 실행합니다. 아니면 null을 반환합니다.
 *
 * ```
 * val result = { true }.ifTrue { "true" } // "true"
 * val result = { false }.ifTrue { "true" } // null
 * ```
 */
inline fun <T: Any> (() -> Boolean).ifTrue(block: () -> T): T? = this().ifTrue(block)

/**
 * 값이 true 라면 [block]을 실행합니다. false라면 null을 반환합니다.
 *
 * ```
 * val result = true.ifFalse { "false" } ?: "true"  // "true"
 * val result = false.ifFalse { "false" } ?: "true" // "false"
 */
inline fun <T: Any> Boolean.ifFalse(block: () -> T): T? = when (this) {
    false -> block()
    else  -> null
}

/**
 * 함수가 false를 반환하면 [block]을 실행합니다. 아니면 null을 반환합니다.
 *
 * ```
 * val result = { true }.ifFalse { "false" } // null
 * val result = { false }.ifFalse { "false" } // "false"
 * ```
 */
inline fun <T: Any> (() -> Boolean).ifFalse(block: () -> T): T? = this().ifFalse(block)


/**
 * 두 개의 boolean 값을 크기를 비교합니다.
 *
 * ```
 * compareBoolean(true, true) // 0
 * compareBoolean(true, false) // 1
 * compareBoolean(false, true) // -1
 * ```
 */
fun compareBoolean(left: Boolean, right: Boolean): Int = when {
    left == right -> 0
    left          -> 1
    else          -> -1
}

/**
 * 값이 true 라면 [other]를 실행하여 반환하고, 아니면 null 을 반환합니다.
 *
 * ```
 * val result = (condition == 1) then { true } ?: false
 * ```
 */
inline fun <T> Boolean.then(other: () -> T): T? = if (this) other() else null

/**
 * 값이 true 라면 [result]를 반환하고, 아니면 null 을 반환합니다.
 *
 * ```
 * val result = (condition == 1) then { true } ?: false
 * ```
 */
fun <T> Boolean.then(result: T): T? = if (this) result else null

/**
 * 값이 true 라면 [other]를 실행하여 반환하고, 아니면 null 을 반환합니다.
 *
 * ```
 * val result = (condition == 1) then { true } ?: false
 * ```
 */
inline fun <T> (() -> Boolean).then(other: () -> T): T? = if (this()) other() else null

/**
 * 값이 true 라면 [result]를 반환하고, 아니면 null 을 반환합니다.
 *
 * ```
 * val result = (condition == 1) then { true } ?: false
 * ```
 */
fun <T> (() -> Boolean).then(result: T): T? = if (this()) result else null

/**
 * False if null
 *
 * ```
 * val x: Boolean? = null
 *
 * x.falseIfNull() // false
 * x.trueIfNull() // true
 * ```
 */
fun Boolean?.falseIfNull(): Boolean = this ?: false

/**
 * True if null
 *
 * ```
 * val x: Boolean? = null
 *
 * x.falseIfNull() // false
 * x.trueIfNull() // true
 * ```
 */
fun Boolean?.trueIfNull(): Boolean = this ?: true
