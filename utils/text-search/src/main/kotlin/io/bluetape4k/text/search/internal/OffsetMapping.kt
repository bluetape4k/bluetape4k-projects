package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.NormalizationForm
import java.text.Normalizer

/**
 * 원본 텍스트와 정규화된 텍스트 간의 문자(`char`) offset 매핑.
 *
 * 유니코드 정규화는 문자열 길이를 변화시킬 수 있으므로, 정규화된 텍스트에서
 * 매치된 offset을 원본 텍스트의 offset으로 되돌리려면 별도의 매핑 테이블이 필요하다.
 *
 * ## 길이 변화 예시
 * - **NFC**: `ㄴㅏ` (2 chars, 자모 분리) → `나` (1 char) — 길이 감소
 * - **NFKC**: `㈜` (1 char) → `(주)` (3 chars) — 길이 증가
 *
 * ## 사용 패턴
 * ```kotlin
 * val (normalized, mapping) = OffsetMapping.build(originalText, NormalizationForm.NFC)
 * val emits = trie.parseText(normalized)
 * emits.forEach { e ->
 *     val origStart = mapping?.toOriginal(e.start) ?: e.start
 *     val origEnd   = mapping?.toOriginalEndInclusive(e.end) ?: e.end
 * }
 * ```
 *
 * @property normToOrig 정규화된 텍스트의 위치 → 원본 텍스트의 위치 (size = normLen + 1, sentinel 포함)
 * @property origToNorm 원본 텍스트의 위치 → 정규화된 텍스트의 위치 (size = origLen + 1, sentinel 포함)
 */
internal class OffsetMapping private constructor(
    private val normToOrig: IntArray,
    private val origToNorm: IntArray,
) {
    /**
     * 정규화된 offset을 원본 offset으로 변환한다.
     *
     * @param normOffset 정규화된 텍스트 내 offset (inclusive)
     * @return 원본 텍스트 내 대응 offset
     */
    fun toOriginal(normOffset: Int): Int =
        when {
            normToOrig.isEmpty() -> 0
            normOffset < 0 -> normToOrig[0]
            normOffset < normToOrig.size -> normToOrig[normOffset]
            else -> normToOrig.last()
        }

    /**
     * 정규화된 텍스트의 inclusive end offset을 원본 텍스트의 inclusive end offset으로 변환한다.
     *
     * `end + 1` (exclusive end)을 원본 exclusive end로 변환한 후 `-1`을 빼는 패턴.
     * Aho-Corasick의 `Emit.end`가 inclusive이므로 별도의 헬퍼가 필요하다.
     *
     * @param normEndInclusive 정규화된 텍스트 내 end offset (inclusive)
     * @return 원본 텍스트 내 end offset (inclusive)
     */
    fun toOriginalEndInclusive(normEndInclusive: Int): Int {
        val normEndExclusive = normEndInclusive + 1
        val origEndExclusive = if (normEndExclusive < normToOrig.size) {
            normToOrig[normEndExclusive]
        } else {
            normToOrig.last()
        }
        return origEndExclusive - 1
    }

    /**
     * 원본 offset을 정규화된 offset으로 변환한다.
     *
     * @param origOffset 원본 텍스트 내 offset (inclusive)
     * @return 정규화된 텍스트 내 대응 offset
     */
    fun toNormalized(origOffset: Int): Int =
        when {
            origToNorm.isEmpty() -> 0
            origOffset < 0 -> origToNorm[0]
            origOffset < origToNorm.size -> origToNorm[origOffset]
            else -> origToNorm.last()
        }

    companion object {
        /**
         * [OffsetMapping]을 구축한다.
         *
         * **알고리즘**: char 단위 sliding 정규화.
         * 각 원본 문자를 개별로 정규화하여 정규화 후 길이를 누적하면서 매핑 배열을 구축한다.
         * 전체 문자열을 한 번에 normalize한 결과와 동일하지만 offset 추적이 가능하다.
         *
         * @param original 원본 [CharSequence]
         * @param form 정규화 형식
         * @return `(정규화된 문자열, OffsetMapping?)` — [NormalizationForm.NONE]이면 mapping은 `null`
         */
        fun build(original: CharSequence, form: NormalizationForm): Pair<String, OffsetMapping?> {
            if (form == NormalizationForm.NONE) {
                return original.toString() to null
            }

            val javaForm = when (form) {
                NormalizationForm.NFC -> Normalizer.Form.NFC
                NormalizationForm.NFKC -> Normalizer.Form.NFKC
                NormalizationForm.NONE -> return original.toString() to null
            }

            val origLen = original.length
            val origToNorm = IntArray(origLen + 1)

            // 빈 입력 단축
            if (origLen == 0) {
                val emptyNorm = IntArray(1)  // [0] sentinel only
                origToNorm[0] = 0
                return "" to OffsetMapping(emptyNorm, origToNorm)
            }

            // 알고리즘: incremental prefix normalization.
            //
            // 매 단계마다 원본 prefix를 한 글자 늘려가며 전체 prefix를 normalize.
            // 정규화 결과 길이의 변화 패턴으로 origPos를 normalized 위치에 매핑한다:
            //
            // - 길이 증가 (확장): 새로 늘어난 normalized 위치는 모두 현재 origPos에서 비롯됨
            // - 길이 감소 (합성): 일부 trailing 위치가 사라짐. 남은 마지막 위치는 합성 결과 → 현재 origPos가 마지막 기여자
            // - 길이 동일: 마지막 위치가 reorder/replacement으로 갱신되었을 수 있으므로 origPos 재기록
            //
            // 결과는 `Normalizer.normalize(original, form)`과 정확히 동일하다 (생성 방식 동일).
            //
            // 복잡도: 최악의 경우 O(n²) — 매 step마다 prefix 전체 정규화.
            // text-search 입력 크기에선 충분하나, 대용량은 ICU4J `Normalizer2.normalizeSecondAndAppend` 권장.
            val sb = StringBuilder(origLen)
            val normToOrigList = ArrayList<Int>(origLen)
            var lastNormLen = 0

            for (origPos in 0 until origLen) {
                origToNorm[origPos] = lastNormLen
                sb.append(original[origPos])
                val curNorm = Normalizer.normalize(sb, javaForm)
                val curLen = curNorm.length
                when {
                    curLen > lastNormLen -> {
                        // 확장: 새로 추가된 normalized 위치는 모두 origPos가 기여
                        repeat(curLen - lastNormLen) { normToOrigList.add(origPos) }
                    }
                    curLen < lastNormLen -> {
                        // 합성으로 길이 감소: 잉여 trailing 매핑 제거 후 마지막 살아남은 위치를 origPos로 갱신
                        while (normToOrigList.size > curLen) {
                            normToOrigList.removeAt(normToOrigList.size - 1)
                        }
                        if (curLen > 0) {
                            normToOrigList[curLen - 1] = origPos
                        }
                    }
                    else -> {
                        // 길이 동일: trailing 위치가 reorder/replace로 갱신될 수 있으므로 origPos 재기록
                        if (curLen > 0) {
                            normToOrigList[curLen - 1] = origPos
                        }
                    }
                }
                lastNormLen = curLen
            }
            origToNorm[origLen] = lastNormLen

            // normalized 결과 — sb 정규화 결과를 그대로 사용
            val normalized = Normalizer.normalize(sb, javaForm)
            check(normalized.length == lastNormLen) {
                "internal: normalized length mismatch (${normalized.length} != $lastNormLen)"
            }

            // normToOrig 배열 + sentinel
            val normToOrig = IntArray(lastNormLen + 1)
            for (i in 0 until lastNormLen) normToOrig[i] = normToOrigList[i]
            normToOrig[lastNormLen] = origLen

            return normalized to OffsetMapping(normToOrig, origToNorm)
        }

        /**
         * Identity 매핑 — `normOffset == origOffset` (정규화 미적용 케이스).
         *
         * @param length 매핑할 텍스트 길이
         */
        fun identity(length: Int): OffsetMapping {
            val arr = IntArray(length + 1) { it }
            return OffsetMapping(arr.copyOf(), arr.copyOf())
        }
    }
}
