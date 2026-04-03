package io.bluetape4k.utils

import io.bluetape4k.utils.Wildcard.match
import io.bluetape4k.utils.Wildcard.matchPath


/**
 * 문자열 또는 경로가 와일드카드 패턴에 매칭되는지 검사합니다.
 *
 * 지원 패턴:
 * - `?`: 임의의 한 문자와 매칭
 * - `*`: 임의의 0개 이상의 문자와 매칭
 * - `\*`, `\?`: 이스케이프된 와일드카드 (리터럴 매칭)
 * - `**`: 경로 매칭 시 깊은 트리 와일드카드 (Ant 스타일)
 *
 * 재귀적 매칭을 사용하며, Linux/Windows 와일드카드와 동일하게 동작합니다.
 */
object Wildcard {

    /**
     * 문자열이 와일드카드 패턴에 매칭되는지 검사합니다.
     *
     * @param string 검사할 입력 문자열
     * @param pattern 와일드카드 패턴
     * @return 매칭되면 `true`, 아니면 `false`
     */
    @JvmStatic
    fun match(string: CharSequence, pattern: CharSequence): Boolean {
        return matchInternal(string, pattern, 0, 0)
    }

    /**
     * 두 문자열이 동일하거나 와일드카드 패턴에 매칭되는지 검사합니다.
     *
     * 동일한 문자열을 많이 비교할 때 성능 최적화에 유용합니다.
     *
     * @param string 검사할 입력 문자열
     * @param pattern 와일드카드 패턴
     * @return 동일하거나 매칭되면 `true`
     */
    @JvmStatic
    fun equalsOrMatch(string: CharSequence, pattern: CharSequence): Boolean {
        if (string == pattern) {
            return true
        }
        return matchInternal(string, pattern, 0, 0)
    }

    /**
     * 여러 패턴 중 하나라도 매칭되는 패턴의 인덱스를 반환합니다.
     *
     * @param src 검사할 입력 문자열
     * @param patterns 와일드카드 패턴 목록
     * @return 매칭된 패턴의 인덱스, 매칭 없으면 `-1`
     * @see match
     */
    @JvmStatic
    fun matchOne(src: String, patterns: List<String>): Int {
        for (i in patterns.indices) {
            if (match(src, patterns[i])) {
                return i
            }
        }
        return -1
    }

    /**
     * 경로가 와일드카드 패턴에 매칭되는지 검사합니다.
     *
     * 경로와 패턴을 경로 구분자(`/`, `\`)로 토큰화한 뒤 매칭합니다.
     * `**`는 Ant 스타일의 깊은 트리 와일드카드로 동작합니다.
     *
     * @param path 검사할 경로 문자열
     * @param pattern 와일드카드 패턴
     * @return 매칭되면 `true`, 아니면 `false`
     */
    @JvmStatic
    fun matchPath(path: String, pattern: String): Boolean {
        val pathElements = path.split('/', '\\')
        val patternElements = pattern.split('/', '\\')
        return matchTokens(pathElements, patternElements)
    }

    /**
     * 여러 경로 패턴 중 하나라도 매칭되는 패턴의 인덱스를 반환합니다.
     *
     * @param path 검사할 경로 문자열
     * @param patterns 경로 와일드카드 패턴 목록
     * @return 매칭된 패턴의 인덱스, 매칭 없으면 `-1`
     * @see matchPath
     */
    @JvmStatic
    fun matchPathOne(path: String, patterns: List<String>): Int {
        for (i in patterns.indices) {
            if (matchPath(path, patterns[i])) {
                return i
            }
        }
        return -1
    }

    // ---------------------------------------------------------------- internal

    private const val PATH_MATCH = "**"

    /**
     * 재귀적 와일드카드 매칭 내부 함수
     */
    @Suppress("NAME_SHADOWING")
    private fun matchInternal(
        string: CharSequence,
        pattern: CharSequence,
        sNdx: Int = 0,
        pNdx: Int = 0,
    ): Boolean {
        var sNdx = sNdx
        var pNdx = pNdx
        val pLen = pattern.length
        if (pLen == 1) {
            if (pattern[0] == '*') {
                return true
            }
        }
        val sLen = string.length
        var nextIsNotWildcard = false

        while (true) {
            // 문자열 끝 검사: 패턴에 남은 '*'만 허용
            if (sNdx >= sLen) {
                while (pNdx < pLen && pattern[pNdx] == '*') {
                    pNdx++
                }
                return pNdx >= pLen
            }
            // 패턴 끝이지만 문자열이 남음
            if (pNdx >= pLen) {
                return false
            }
            val p = pattern[pNdx]

            if (!nextIsNotWildcard) {
                if (p == '\\') {
                    pNdx++
                    nextIsNotWildcard = true
                    continue
                }
                if (p == '?') {
                    sNdx++
                    pNdx++
                    continue
                }
                if (p == '*') {
                    var pNext: Char = 0.toChar()
                    if (pNdx + 1 < pLen) {
                        pNext = pattern[pNdx + 1]
                    }
                    if (pNext == '*') {
                        pNdx++
                        continue
                    }
                    pNdx++

                    // 문자열 끝부터 역방향으로 재귀 매칭 시도
                    var i = string.length
                    while (i >= sNdx) {
                        if (matchInternal(string, pattern, i, pNdx)) {
                            return true
                        }
                        i--
                    }
                    return false
                }
            } else {
                nextIsNotWildcard = false
            }

            // 현재 문자 비교
            if (p != string[sNdx]) {
                return false
            }

            sNdx++
            pNdx++
        }
    }

    /**
     * 토큰화된 경로와 패턴을 매칭합니다.
     */
    private fun matchTokens(tokens: List<String>, patterns: List<String>): Boolean {
        var patNdxStart = 0
        var patNdxEnd = patterns.size - 1
        var tokNdxStart = 0
        var tokNdxEnd = tokens.size - 1

        // 첫 번째 ** 찾기
        while (patNdxStart <= patNdxEnd && tokNdxStart <= tokNdxEnd) {
            val patDir = patterns[patNdxStart]
            if (patDir == PATH_MATCH) {
                break
            }
            if (!match(tokens[tokNdxStart], patDir)) {
                return false
            }
            patNdxStart++
            tokNdxStart++
        }
        if (tokNdxStart > tokNdxEnd) {
            for (i in patNdxStart..patNdxEnd) {
                if (patterns[i] != PATH_MATCH) {
                    return false
                }
            }
            return true
        }
        if (patNdxStart > patNdxEnd) {
            return false
        }

        // 마지막 ** 찾기
        while (patNdxStart <= patNdxEnd && tokNdxStart <= tokNdxEnd) {
            val patDir = patterns[patNdxEnd]
            if (patDir == PATH_MATCH) {
                break
            }
            if (!match(tokens[tokNdxEnd], patDir)) {
                return false
            }
            patNdxEnd--
            tokNdxEnd--
        }
        if (tokNdxStart > tokNdxEnd) {
            for (i in patNdxStart..patNdxEnd) {
                if (patterns[i] != PATH_MATCH) {
                    return false
                }
            }
            return true
        }

        // 중간 ** 처리
        while (patNdxStart != patNdxEnd && tokNdxStart <= tokNdxEnd) {
            var patIdxTmp = -1
            for (i in patNdxStart + 1..patNdxEnd) {
                if (patterns[i] == PATH_MATCH) {
                    patIdxTmp = i
                    break
                }
            }
            if (patIdxTmp == patNdxStart + 1) {
                patNdxStart++
                continue
            }
            val patLength = patIdxTmp - patNdxStart - 1
            val strLength = tokNdxEnd - tokNdxStart + 1
            var ndx = -1
            strLoop@ for (i in 0..strLength - patLength) {
                for (j in 0 until patLength) {
                    val subPat = patterns[patNdxStart + j + 1]
                    val subStr = tokens[tokNdxStart + i + j]
                    if (!match(subStr, subPat)) {
                        continue@strLoop
                    }
                }
                ndx = tokNdxStart + i
                break
            }

            if (ndx == -1) {
                return false
            }

            patNdxStart = patIdxTmp
            tokNdxStart = ndx + patLength
        }

        for (i in patNdxStart..patNdxEnd) {
            if (patterns[i] != PATH_MATCH) {
                return false
            }
        }

        return true
    }
}
