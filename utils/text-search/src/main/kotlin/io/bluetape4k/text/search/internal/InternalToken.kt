package io.bluetape4k.text.search.internal

import java.io.Serializable

/**
 * 토크나이징 결과 조각을 표현하는 공통 계약입니다.
 *
 * ## 동작/계약
 * - [fragment]는 원문 일부 문자열입니다.
 * - [emit]이 null이 아니면 키워드 매칭 토큰을 의미합니다.
 *
 * ```kotlin
 * val token: InternalToken = MatchToken("PM", Emit(0, 1, "PM"))
 * // token.isMatch() == true
 * ```
 *
 * @property fragment 문장의 조각 (키워드)
 * @property emit Emit 정보
 */
internal sealed interface InternalToken: Serializable {
    val fragment: String
    val emit: Emit?

    fun isMatch(): Boolean
}

internal abstract class AbstractInternalToken(override val fragment: String): InternalToken {
    override fun toString(): String = "InternalToken(fragment=$fragment, emit=$emit)"
}

/**
 * 키워드를 포함한 Emit 을 나타내는 InternalToken
 *
 * ## 동작/계약
 * - 항상 [isMatch]가 `true`입니다.
 *
 * @property emit Emit 정보
 */
internal class MatchToken(fragment: String, override val emit: Emit): AbstractInternalToken(fragment) {
    override fun isMatch(): Boolean = true
}

/**
 * 키워드를 포함하지 않는 Emit을 나타내는 InternalToken
 *
 * ## 동작/계약
 * - 항상 [isMatch]가 `false`이며 [emit]은 null입니다.
 */
internal class FragmentToken(fragment: String): AbstractInternalToken(fragment) {
    override fun isMatch(): Boolean = false
    override val emit: Emit? = null
}
