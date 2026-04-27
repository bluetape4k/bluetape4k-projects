package io.bluetape4k.text.search.internal

import io.bluetape4k.text.search.NormalizationForm
import io.bluetape4k.text.search.SearchOptions
import java.text.Normalizer
import java.util.Locale

/**
 * 텍스트에 [SearchOptions]의 정규화 파이프라인을 적용한다.
 *
 * 적용 순서:
 * 1. [SearchOptions.normalization]에 따라 NFC/NFKC 정규화
 * 2. [SearchOptions.ignoreCase]가 `true`이면 [Locale.ROOT] 기준 소문자 변환
 *
 * 이 함수는 키워드 등록(빌드 시점)과 검색 텍스트 전처리 모두에 동일하게 사용되어야
 * 매치 일관성이 보장된다.
 *
 * ```kotlin
 * val opts = SearchOptions(ignoreCase = true, normalization = NormalizationForm.NFC)
 * val normalized = applyPipeline("APPLE", opts) // "apple"
 * ```
 *
 * @param s 정규화할 입력 문자열
 * @param opts 적용할 검색 옵션
 * @return 정규화 + 케이스 변환이 적용된 문자열
 */
internal fun applyPipeline(s: CharSequence, opts: SearchOptions): String {
    val step1 = when (opts.normalization) {
        NormalizationForm.NONE -> s.toString()
        NormalizationForm.NFC -> Normalizer.normalize(s, Normalizer.Form.NFC)
        NormalizationForm.NFKC -> Normalizer.normalize(s, Normalizer.Form.NFKC)
    }
    return if (opts.ignoreCase) step1.lowercase(Locale.ROOT) else step1
}
