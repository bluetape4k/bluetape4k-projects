package io.bluetape4k.examples.coroutines.guide

/**
 * 이벤트 버스 예제에서 사용하는 sealed 이벤트 클래스입니다.
 *
 * [data object]를 사용하여 `toString()` 자동 생성, `equals`/`hashCode` 지원을 활용합니다.
 */
sealed class Event {

    /** 생성 이벤트 */
    data object Created: Event()

    /** 삭제 이벤트 */
    data object Deleted: Event()
}
