package io.bluetape4k.ahocorasick.trie

import java.io.Serializable

/**
 * 토크나이징 결과 조각을 표현하는 공통 계약입니다.
 *
 * ## 동작/계약
 * - [fragment]는 원문 일부 문자열입니다.
 * - [emit]이 null이 아니면 키워드 매칭 토큰을 의미합니다.
 *
 * ```kotlin
 * val token: Token = MatchToken("PM", Emit(0, 1, "PM"))
 * // token.isMatch() == true
 * ```
 *
 * @property fragment 문장의 조각 (키워드)
 * @property emit Emit 정보
 */
interface Token: Serializable {
    val fragment: String
    val emit: Emit?

    fun isMatch(): Boolean
}

abstract class AbstractToken(override val fragment: String): Token {
    override fun toString(): String = "Token(fragment=$fragment, emit=$emit)"
}

/**
 * 키워드를 포함한 Emit 을 나타내는 Token
 *
 * ## 동작/계약
 * - 항상 [isMatch]가 `true`입니다.
 *
 * @property emit Emit 정보
 */
class MatchToken(fragment: String, override val emit: Emit): AbstractToken(fragment) {
    override fun isMatch(): Boolean = true
}

/**
 * 키워드를 포함하지 않는 Emit을 나타내는 Token
 *
 * ## 동작/계약
 * - 항상 [isMatch]가 `false`이며 [emit]은 null입니다.
 */
class FragmentToken(fragment: String): AbstractToken(fragment) {
    override fun isMatch(): Boolean = false
    override val emit: Emit? = null
}
