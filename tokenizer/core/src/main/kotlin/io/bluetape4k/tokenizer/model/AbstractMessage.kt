package io.bluetape4k.tokenizer.model

import java.io.Serializable

/**
 * REST API 요청, 응답 Message의 최상위 추상화 클래스
 */
abstract class AbstractMessage: Serializable {

    /**
     * Message 생성 시각
     */
    val timestamp: Long = System.currentTimeMillis()

}
