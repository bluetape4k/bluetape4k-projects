package io.bluetape4k.tokenizer.model

import java.io.Serializable

/**
 * 토크나이저 요청/응답 메시지의 공통 타임스탬프를 제공하는 추상 타입이다.
 *
 * ## 동작/계약
 * - 인스턴스 생성 시점의 `System.currentTimeMillis()` 값을 `timestamp`에 저장한다.
 * - `timestamp`는 `val`이므로 생성 이후 변경되지 않는다.
 * - 직렬화 가능한 메시지 계층을 위해 `Serializable`을 구현한다.
 *
 * ```kotlin
 * val request = tokenizeRequestOf("안녕")
 * val response = tokenizeResponseOf(request.text, listOf("안녕"))
 * // request.timestamp > 0L
 * // response.timestamp >= request.timestamp
 * ```
 */
abstract class AbstractMessage: Serializable {

    /**
     * 메시지 인스턴스가 생성된 시각(밀리초 epoch)이다.
     *
     * ## 동작/계약
     * - 객체 생성 시 한 번만 초기화된다.
     * - 같은 인스턴스에서 반복 조회해도 값이 변하지 않는다.
     *
     * ```kotlin
     * val message = tokenizeRequestOf("문장")
     * val first = message.timestamp
     * val second = message.timestamp
     * // first == second
     * ```
     */
    val timestamp: Long = System.currentTimeMillis()

}
