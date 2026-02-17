package io.bluetape4k.tokenizer.japanese.tokenizer

import com.atilika.kuromoji.TokenBase

/**
 * 형태소 분석 결과에서 명사(名詞)인지 확인합니다.
 */
fun TokenBase.isNoun(): Boolean = this.allFeaturesArray[0] == "名詞"

/**
 * 형태소 분석 결과에서 동사(動詞)인지 확인합니다.
 */
fun TokenBase.isVerb(): Boolean = this.allFeaturesArray[0] == "動詞"

/**
 * 형태소 분석 결과에서 명사 또는 동사인지 확인합니다.
 */
fun TokenBase.isNounOrVerb(): Boolean = this.isNoun() || this.isVerb()

/**
 * 형태소 분석 결과에서 형용사(形容詞)인지 확인합니다.
 */
fun TokenBase.isAdjective(): Boolean = this.allFeaturesArray[0] == "形容詞"

/**
 * 형태소 분석 결과에서 조사(助詞)인지 확인합니다.
 */
fun TokenBase.isJosa(): Boolean = this.allFeaturesArray[0] == "助詞"

/**
 * 형태소 분석 결과에서 조동사(助動詞)인지 확인합니다.
 */
fun TokenBase.isConjugate(): Boolean = this.allFeaturesArray[0] == "助動詞"

/**
 * 형태소 분석 결과에서 기호(記号)인지 확인합니다.
 */
fun TokenBase.isPunctuation(): Boolean = this.allFeaturesArray[0] == "記号"
