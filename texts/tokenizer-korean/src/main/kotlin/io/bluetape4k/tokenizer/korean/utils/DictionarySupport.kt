package io.bluetape4k.tokenizer.korean.utils

/**
 * 이름 사전의 특정 분류에 [charseq]가 포함되는지 확인합니다.
 *
 * ## 동작/계약
 * - `KoreanDictionaryProvider.nameDictionary[key]`가 없으면 `false`를 반환한다.
 * - 사전이 있으면 `CharArraySet.contains(CharSequence)` 결과를 그대로 반환한다.
 *
 * ```kotlin
 * val exists = nameDictionaryContains("family_name", "김")
 * // exists == true 또는 false
 * ```
 */
fun nameDictionaryContains(key: String, charseq: CharSequence): Boolean =
    KoreanDictionaryProvider.nameDictionary[key]?.contains(charseq) ?: false

/**
 * 이름 사전의 특정 분류에 [str]이 포함되는지 확인합니다.
 *
 * ## 동작/계약
 * - `KoreanDictionaryProvider.nameDictionary[key]`가 없으면 `false`를 반환한다.
 * - 사전이 있으면 `CharArraySet.contains(String)` 결과를 그대로 반환한다.
 *
 * ```kotlin
 * val exists = nameDictionaryContains("full_name", "문재인")
 * // exists == true 또는 false
 * ```
 */
fun nameDictionaryContains(key: String, str: String): Boolean =
    KoreanDictionaryProvider.nameDictionary[key]?.contains(str) ?: false

/**
 * 품사별 사전에 [cs]가 포함되는지 확인합니다.
 *
 * ## 동작/계약
 * - `KoreanDictionaryProvider.koreanDictionary[pos]`가 없으면 `false`를 반환한다.
 * - 사전이 있으면 `CharArraySet.contains(CharSequence)` 결과를 그대로 반환한다.
 *
 * ```kotlin
 * val exists = koreanContains(KoreanPos.Noun, "사랑")
 * // exists == true 또는 false
 * ```
 */
fun koreanContains(pos: KoreanPos, cs: CharSequence): Boolean =
    KoreanDictionaryProvider.koreanDictionary[pos]?.contains(cs) ?: false
