package io.bluetape4k.ahocorasick.trie

import java.io.Serializable

/**
 * Trie의 설정을 나타내는 데이터 클래스.
 *
 * ## 동작/계약
 * - 검색 겹침 허용, 단어 경계, 대소문자 무시, 조기 종료 옵션을 보관합니다.
 * - mutable 프로퍼티이므로 런타임 변경 시 Trie 동작이 달라질 수 있습니다.
 *
 * ```kotlin
 * val config = TrieConfig.builder().ignoreCase(true).onlyWholeWords(true).build()
 * // config.ignoreCase == true
 * ```
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

        /** [allowOverlaps] 값을 설정합니다. */
        fun allowOverlaps(value: Boolean = true) = apply {
            this.allowOverlaps = value
        }

        /** [onlyWholeWords] 값을 설정합니다. */
        fun onlyWholeWords(value: Boolean = false) = apply {
            this.onlyWholeWords = value
        }

        /** [onlyWholeWordsWhiteSpaceSeparated] 값을 설정합니다. */
        fun onlyWholeWordsWhiteSpaceSeparated(value: Boolean = false) = apply {
            this.onlyWholeWordsWhiteSpaceSeparated = value
        }

        /** [ignoreCase] 값을 설정합니다. */
        fun ignoreCase(value: Boolean = false) = apply {
            this.ignoreCase = value
        }

        /** [stopOnHit] 값을 설정합니다. */
        fun stopOnHit(value: Boolean = false) = apply {
            this.stopOnHit = value
        }

        /** 현재 설정으로 [TrieConfig]를 생성합니다. */
        fun build(): TrieConfig {
            return TrieConfig(allowOverlaps, onlyWholeWords, onlyWholeWordsWhiteSpaceSeparated, ignoreCase, stopOnHit)
        }
    }
}
