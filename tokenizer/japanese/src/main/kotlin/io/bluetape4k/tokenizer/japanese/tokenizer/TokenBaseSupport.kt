package io.bluetape4k.tokenizer.japanese.tokenizer

import com.atilika.kuromoji.TokenBase

/**
 * 토큰의 1차 품사(`allFeaturesArray[0]`)가 명사(`名詞`)인지 판별합니다.
 *
 * ## 동작/계약
 * - 판별 기준은 `allFeaturesArray[0] == "名詞"` 비교입니다.
 * - 형태소 사전의 세부 품사 레벨(2~4)은 사용하지 않습니다.
 * - 불리언만 반환하며 토큰 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "寿司" }.isNoun()
 *
 * // value == true
 * ```
 */
fun TokenBase.isNoun(): Boolean = this.allFeaturesArray[0] == "名詞"

/**
 * 토큰의 1차 품사(`allFeaturesArray[0]`)가 동사(`動詞`)인지 판별합니다.
 *
 * ## 동작/계약
 * - 판별 기준은 `allFeaturesArray[0] == "動詞"` 비교입니다.
 * - 활용형/원형 정보와 무관하게 1차 품사만 확인합니다.
 * - 불리언만 반환하며 토큰 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "食べ" }.isVerb()
 *
 * // value == true
 * ```
 */
fun TokenBase.isVerb(): Boolean = this.allFeaturesArray[0] == "動詞"

/**
 * 토큰이 명사(`名詞`) 또는 동사(`動詞`)인 경우 `true`를 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `isNoun() || isVerb()`를 순서대로 평가합니다.
 * - 품사 1차 분류 외 정보는 고려하지 않습니다.
 * - 불리언만 반환하며 토큰 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "が" }.isNounOrVerb()
 *
 * // value == false
 * ```
 */
fun TokenBase.isNounOrVerb(): Boolean = this.isNoun() || this.isVerb()

/**
 * 토큰의 1차 품사가 형용사(`形容詞`)인지 판별합니다.
 *
 * ## 동작/계약
 * - 판별 기준은 `allFeaturesArray[0] == "形容詞"` 비교입니다.
 * - 불리언만 반환하며 토큰 상태를 변경하지 않습니다.
 * - Kuromoji 품사 체계 변경 시 결과도 해당 값에 따라 달라집니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("美しい花")
 * val value = tokens.first { it.surface == "美しい" }.isAdjective()
 *
 * // value == true
 * ```
 */
fun TokenBase.isAdjective(): Boolean = this.allFeaturesArray[0] == "形容詞"

/**
 * 토큰의 1차 품사가 조사(`助詞`)인지 판별합니다.
 *
 * ## 동작/계약
 * - 판별 기준은 `allFeaturesArray[0] == "助詞"` 비교입니다.
 * - 조사 세부 타입(격조사/종조사 등)은 별도로 구분하지 않습니다.
 * - 불리언만 반환하며 토큰 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "が" }.isJosa()
 *
 * // value == true
 * ```
 */
fun TokenBase.isJosa(): Boolean = this.allFeaturesArray[0] == "助詞"

/**
 * 토큰의 1차 품사가 조동사(`助動詞`)인지 판별합니다.
 *
 * ## 동작/계약
 * - 판별 기준은 `allFeaturesArray[0] == "助動詞"` 비교입니다.
 * - 활용형 문자열은 참고하지 않고 품사 코드만 사용합니다.
 * - 불리언만 반환하며 토큰 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "たい" }.isConjugate()
 *
 * // value == true
 * ```
 */
fun TokenBase.isConjugate(): Boolean = this.allFeaturesArray[0] == "助動詞"

/**
 * 토큰의 1차 품사가 기호(`記号`)인지 판별합니다.
 *
 * ## 동작/계약
 * - 판별 기준은 `allFeaturesArray[0] == "記号"` 비교입니다.
 * - 문장부호 외 기호 계열 토큰도 품사값이 같으면 `true`를 반환합니다.
 * - 불리언만 반환하며 토큰 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val tokens = JapaneseTokenizer.tokenize("お寿司が食べたい。")
 * val value = tokens.first { it.surface == "。" }.isPunctuation()
 *
 * // value == true
 * ```
 */
fun TokenBase.isPunctuation(): Boolean = this.allFeaturesArray[0] == "記号"
