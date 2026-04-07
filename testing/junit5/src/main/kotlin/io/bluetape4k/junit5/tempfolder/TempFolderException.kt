package io.bluetape4k.junit5.tempfolder

/**
 * [TempFolder] 생성/정리 과정의 실패를 표현하는 런타임 예외입니다.
 *
 * ## 동작/계약
 * - 파일 시스템 I/O 예외를 래핑해 테스트 코드로 전달할 때 사용됩니다.
 * - checked 예외가 아니므로 호출부에서 선택적으로 처리할 수 있습니다.
 *
 * ```kotlin
 * throw TempFolderException("임시 파일 생성 실패")
 * // 예외 타입 == TempFolderException
 * ```
 */
open class TempFolderException: RuntimeException {
    /** 메시지와 원인 없이 예외를 생성합니다. */
    constructor(): super()

    /** 메시지만 지정해 예외를 생성합니다. */
    constructor(msg: String): super(msg)

    /** 메시지와 원인을 함께 지정해 예외를 생성합니다. */
    constructor(msg: String, cause: Throwable?): super(msg, cause)

    /** 원인만 지정해 예외를 생성합니다. */
    constructor(cause: Throwable?): super(cause)
}
