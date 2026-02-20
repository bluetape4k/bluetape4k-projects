package io.bluetape4k.support.i18n

import io.bluetape4k.support.emptyCharArray

/**
 * 한글 자모 범위 (ㄱ~ㅎ, ㅏ~ㅣ)
 */
private const val JAMO_START = 0x3131
private const val JAMO_END = 0x3163
private val JAMO_RANGE = JAMO_START..JAMO_END

/**
 * 한글 완성형 범위 (가~힣)
 */
private const val KOREAN_START = 0xAC00
private const val KOREAN_END = 0xD7AF
val KOREAN_RANGE = KOREAN_START..KOREAN_END

// @formatter:off
private val CHO_SUNG = intArrayOf(
    0x3131, 0x3132, 0x3134, 0x3137, 0x3138, 0x3139,
    0x3141, 0x3142, 0x3143, 0x3145, 0x3146, 0x3147, 0x3148, 0x3149,
    0x314a, 0x314b, 0x314c, 0x314d, 0x314e
)

private val JUNG_SUNG = intArrayOf(
    0x314f, 0x3150, 0x3151, 0x3152,
    0x3153, 0x3154, 0x3155, 0x3156, 0x3157, 0x3158, 0x3159,
    0x315a, 0x315b, 0x315c, 0x315d, 0x315e, 0x315f, 0x3160, 0x3161, 0x3162, 0x3163
)

private val JONG_SUNG = intArrayOf(
    0x0000, 0x3131, 0x3132, 0x3133, 0x3134, 0x3135,
    0x3136, 0x3137, 0x3139, 0x313a, 0x313b, 0x313c, 0x313d, 0x313e,
    0x313f, 0x3140, 0x3141, 0x3142, 0x3144, 0x3145, 0x3146,
    0x3147, 0x3148, 0x314a, 0x314b, 0x314c, 0x314d, 0x314e
)
// @formatter:on

/**
 * 문자열에 한글(완성형 또는 자모)이 포함되어 있는지 확인합니다.
 *
 * ```
 * "한글".containKorean()   // true
 * "한글a".containKorean()  // true
 * "abc".containKorean()    // false
 * "ㄱㄴㄷ".containKorean() // true
 * ```
 *
 * @receiver 검사할 문자열
 * @return 한글이 포함되어 있으면 true
 */
fun String.containKorean(): Boolean = any { ch ->
    val code = ch.code
    code in KOREAN_RANGE || code in JAMO_RANGE
}

/**
 * 문자열 내의 한글을 자소(초/중/종성)로 분해합니다.
 * 한글이 아닌 문자는 그대로 유지됩니다.
 *
 * ```
 * "한국".getJasoLetter()     // "ㅎㅏㄴㄱㅜㄱ"
 * "한국abc".getJasoLetter()  // "ㅎㅏㄴㄱㅜㄱabc"
 * ```
 */
fun String.getJasoLetter(): String {
    if (this.isBlank()) return ""

    val letters = StringBuilder(this.length * 3)

    for (ch in this) {
        val code = ch.code
        if (code in KOREAN_RANGE) {
            val (choIndex, jungIndex, jongIndex) = code.getIndexes()
            letters.append(CHO_SUNG[choIndex].toChar())
            letters.append(JUNG_SUNG[jungIndex].toChar())
            if (jongIndex != 0) {
                letters.append(JONG_SUNG[jongIndex].toChar())
            }
        } else {
            letters.append(ch)
        }
    }
    return letters.toString()
}

/**
 * 문자열에서 한글의 초성만 추출합니다.
 * 한글이 아닌 문자는 무시됩니다.
 *
 * ```
 * "대한민국".getChosung()        // charArrayOf('ㄷ', 'ㅎ', 'ㅁ', 'ㄱ')
 * "대한민국abc".getChosung()     // charArrayOf('ㄷ', 'ㅎ', 'ㅁ', 'ㄱ')
 * ```
 */
fun String.getChosung(): CharArray {
    if (this.isBlank()) return emptyCharArray

    val chosungs = StringBuilder(this.length)

    for (ch in this) {
        val code = ch.code
        if (code in KOREAN_RANGE) {
            val (choIndex, _, _) = code.getIndexes()
            chosungs.append(CHO_SUNG[choIndex].toChar())
        }
    }
    return chosungs.toString().toCharArray()
}

private fun Int.getIndexes(): Triple<Int, Int, Int> {
    val initIndex = this - 0xAC00

    val jongIndex = initIndex % 28
    val jungIndex = ((initIndex - jongIndex) / 28) % 21
    val choIndex = ((initIndex / 28) - jungIndex) / 21

    return Triple(choIndex, jungIndex, jongIndex)
}
