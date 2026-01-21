package io.bluetape4k.tokenizer.japanese.tokenizer

import com.atilika.kuromoji.TokenBase

/**
 * 형태소 분석 결과에서 명사인지 확인합니다.
 */
fun TokenBase.isNoun(): Boolean = this.allFeatures.contains("名詞")

/**
 * 형태소 분석 결과에서 동사인지 확인합니다.
 */
fun TokenBase.isVerb(): Boolean = this.allFeatures.contains("動詞")

/**
 * 형태소 분석 결과에서 명사 또는 동사인지 확인합니다.
 */
fun TokenBase.isNounOrVerb(): Boolean = this.isNoun() || this.isVerb()

/**
 * 형태소 분석 결과에서 형용사인지 확인합니다.
 */
fun TokenBase.isAdjective(): Boolean = this.allFeatures.contains("形容詞")

/**
 * 형태소 분석 결과에서 부사인지 확인합니다.
 */
fun TokenBase.isJosa(): Boolean = this.allFeatures.contains("助詞")

/**
 * 형태소 분석 결과에서 조사인지 확인합니다.
 */
fun TokenBase.isConjugate(): Boolean = this.allFeatures.contains("助動詞")

/**
 * 형태소 분석 결과에서 어미인지 확인합니다.
 */
fun TokenBase.isPunctuation(): Boolean = this.allFeatures.contains("記号")
