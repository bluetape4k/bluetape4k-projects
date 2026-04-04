package io.bluetape4k.exceptions

/**
 * 지원하지 않는 기능을 호출했을 때 발생하는 예외입니다.
 *
 * 인터페이스나 추상 클래스에서 특정 구현체가 해당 연산을 지원하지 않을 때 사용합니다.
 *
 * ```kotlin
 * class ReadOnlyRepository : BaseRepository() {
 *     override fun save(entity: Any) {
 *         throw NotSupportedException("읽기 전용 저장소는 save를 지원하지 않습니다.")
 *     }
 * }
 * ```
 */
open class NotSupportedException: BluetapeException {
    constructor(): super()
    constructor(msg: String): super(msg)
    constructor(msg: String, cause: Throwable?): super(msg, cause)
    constructor(cause: Throwable?): super(cause)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
