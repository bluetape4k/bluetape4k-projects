package io.bluetape4k.support

/**
 * 루트 예외 메시지에 [message]를 앞에 추가한 메시지를 만듭니다.
 *
 * ```
 * val ex = IllegalStateException("Something went wrong")
 * val message = ex.buildMessage("Failed to do something")
 * // Failed to do something; nested exception is java.lang.IllegalStateException: Something went wrong
 * ```
 *
 * @param message the base message
 * @return the full exception message
 */
fun Throwable?.buildMessage(message: String?): String? {
    if (this == null) {
        return message
    }
    return buildString {
        message?.let { append(it).append("; ") }
        append("nested exception is ").append(cause ?: "not exists")
    }
}

/**
 * [Throwable]의 가장 내부적인 원인(근본 원인)을 검색합니다.
 *
 * ```
 * try {
 *    // ...
 *    throw new IllegalArgumentException("Something went wrong", new IllegalStateException("Something went wrong"))
 *    // ...
 *    throw new IllegalArgumentException("Something went wrong")
 *    // ...
 * } catch (e: IllegalArgumentException) {
 *   e.getRootCause() // IllegalStateException: Something went wrong
 * }
 * ```
 *
 * @return 가장 내부의 원인 (없는 경우 `null`)
 */
fun Throwable.getRootCause(): Throwable? {
    var rootCause: Throwable? = null
    var cause = this.cause
    while (cause != null && cause != rootCause) {
        rootCause = cause
        cause = cause.cause
    }
    return rootCause
}

/**
 * 주어진 예외의 가장 구체적인 원인, 즉 가장 내부적인 원인(근본 원인) 또는 예외 자체를 검색합니다.
 *
 * 근본 원인이 없는 경우 원래 예외로 되돌아간다는 점에서 [getRootCause]와 다릅니다.
 *
 * ```
 * try {
 *   // ...
 *   throw new IllegalArgumentException("Something went wrong", new IllegalStateException("Something went wrong"))
 *   // ...
 *   throw new IllegalArgumentException("Something went wrong")
 *   // ...
 * } catch (e: IllegalArgumentException) {
 *  e.getMostSpecificCause() // IllegalStateException: Something went wrong
 * }
 * ```
 *
 * ```
 * try {
 *     // ...
 *     throw new IllegalArgumentException("Something went wrong")
 * } catch (e: IllegalArgumentException) {
 *     e.getMostSpecificCause() // IllegalArgumentException: Something went wrong
 *  }
 *  ```
 *
 * @return the most specific cause or this. (never `null`)
 */
fun Throwable.getMostSpecificCause(): Throwable = getRootCause() ?: this
