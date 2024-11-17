package io.bluetape4k.ahocorasick.trie

import java.io.Serializable

/**
 * Aho-Corasick 알고리즘의 키워드를 포함하는 Emit을 나타내는 데이터 클래스.
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
 * @property emit Emit 정보
 */
class MatchToken(fragment: String, override val emit: Emit): AbstractToken(fragment) {
    override fun isMatch(): Boolean = true
}

/**
 * 키워드를 포함하지 않는 Emit을 나타내는 Token
 */
class FragmentToken(fragment: String): AbstractToken(fragment) {
    override fun isMatch(): Boolean = false
    override val emit: Emit? = null
}
