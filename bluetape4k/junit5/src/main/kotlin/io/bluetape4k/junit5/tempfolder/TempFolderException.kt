package io.bluetape4k.junit5.tempfolder

/**
 * [TempFolder] 작업 중 발생하는 예외 정보를 담는 클래스입니다.
 */
open class TempFolderException: RuntimeException {
    constructor(): super()
    constructor(msg: String): super(msg)
    constructor(msg: String, cause: Throwable?): super(msg, cause)
    constructor(cause: Throwable?): super(cause)
}
