package io.bluetape4k.ahocorasick.trie

import java.io.Serializable

/**
 * Trie의 설정을 나타내는 데이터 클래스.
 *
 * @property allowOverlaps 중첩을 허용할지 여부
 * @property onlyWholeWords 전체 단어만 찾을지 여부
 * @property onlyWholeWordsWhiteSpaceSeparated 공백으로 구분된 전체 단어만 찾을지 여부
 * @property ignoreCase 대소문자 구분 여부
 * @property stopOnHit 발견 시 중단 여부
 */
data class TrieConfig(
    var allowOverlaps: Boolean = true,
    var onlyWholeWords: Boolean = false,
    var onlyWholeWordsWhiteSpaceSeparated: Boolean = false,
    var ignoreCase: Boolean = false,
    var stopOnHit: Boolean = false,
): Serializable {

    companion object {
        @JvmStatic
        val DEFAULT = TrieConfig()

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var allowOverlaps: Boolean = true
        private var onlyWholeWords: Boolean = false
        private var onlyWholeWordsWhiteSpaceSeparated: Boolean = false
        private var ignoreCase: Boolean = false
        private var stopOnHit: Boolean = false

        fun allowOverlaps(value: Boolean = true) = apply {
            this.allowOverlaps = value
        }

        fun onlyWholeWords(value: Boolean = false) = apply {
            this.onlyWholeWords = value
        }

        fun onlyWholeWordsWhiteSpaceSeparated(value: Boolean = false) = apply {
            this.onlyWholeWordsWhiteSpaceSeparated = value
        }

        fun ignoreCase(value: Boolean = false) = apply {
            this.ignoreCase = value
        }

        fun stopOnHit(value: Boolean = false) = apply {
            this.stopOnHit = value
        }

        fun build(): TrieConfig {
            return TrieConfig(allowOverlaps, onlyWholeWords, onlyWholeWordsWhiteSpaceSeparated, ignoreCase, stopOnHit)
        }
    }
}
