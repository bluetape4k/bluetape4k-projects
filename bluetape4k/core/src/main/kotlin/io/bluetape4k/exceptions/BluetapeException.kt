package io.bluetape4k.exceptions

/**
 * Bluetape4k 라이브러리의 기본 예외 클래스입니다.
 *
 * 라이브러리 전용 예외를 정의할 때 이 클래스를 상속하여 사용합니다.
 *
 * ```kotlin
 * // 직접 사용
 * throw BluetapeException("처리에 실패했습니다.")
 *
 * // 원인 예외 포함
 * try {
 *     riskyOperation()
 * } catch (e: IOException) {
 *     throw BluetapeException("IO 오류 발생", e)
 * }
 *
 * // 서브클래스 정의
 * class MyModuleException(msg: String) : BluetapeException(msg)
 * ```
 */
open class BluetapeException: RuntimeException {
    constructor(): super()
    constructor(msg: String): super(msg)
    constructor(msg: String, cause: Throwable?): super(msg, cause)
    constructor(cause: Throwable?): super(cause)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
