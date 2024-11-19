package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.unsafeLazy
import io.bluetape4k.tokenizer.korean.utils.Hangul.composeHangul
import io.bluetape4k.tokenizer.korean.utils.Hangul.decomposeHangul
import io.bluetape4k.tokenizer.korean.utils.Hangul.hasCoda
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryProvider

/**
 * 한글 동사 및 형용사를 모든 가능한 활용 형태로 확장합니다.
 *
 * 동사
 * ```
 * 갈아타	 --> 갈아타, 갈아타게, 갈아타겠, 갈아타고, 갈아타구, 갈아타기, 갈아타긴, 갈아타길, 갈아타냐, 갈아타네, 갈아타노, 갈아타느, 갈아타는, 갈아타니, 갈아타다, 갈아타더, 갈아타던, 갈아타도, 갈아타든, 갈아타러, 갈아타려, 갈아타며, 갈아타면, 갈아타서, 갈아타세, 갈아타셔, 갈아타셨, 갈아타습, 갈아타시, 갈아타신, 갈아타실, 갈아타십, 갈아타써, 갈아타야, 갈아타자, 갈아타잖, 갈아타재, 갈아타져, 갈아타죠, 갈아타준, 갈아타지, 갈아타진, 갈아타질, 갈아탄, 갈아탈, 갈아탐, 갈아탑, 갈아탔
 * 난리치	 --> 난리쳐, 난리쳤, 난리치, 난리치냐, 난리치노, 난리치느, 난리치는, 난리치니, 난리치어, 난리치었, 난리친, 난리칠, 난리침, 난리칩, 난리칩니
 * ```
 *
 * 형용사
 * ```
 * 거북하	 --> 거북하, 거북하게, 거북하겠, 거북하고, 거북하구, 거북하기, 거북하긴, 거북하길, 거북하냐, 거북하네, 거북하노, 거북하느, 거북하는, 거북하니, 거북하다, 거북하더, 거북하던, 거북하도, 거북하든, 거북하러, 거북하려, 거북하며, 거북하면, 거북하세, 거북하셔, 거북하셨, 거북하습, 거북하시, 거북하신, 거북하실, 거북하십, 거북하여, 거북하였, 거북하자, 거북하잖, 거북하재, 거북하져, 거북하죠, 거북하지, 거북하진, 거북하질, 거북한, 거북할, 거북함, 거북합, 거북해, 거북해도, 거북해서, 거북해써, 거북해야, 거북해준, 거북했, 거북히
 * 느리 -->	느려, 느렸, 느리, 느리냐, 느리노, 느리느, 느리는, 느리니, 느리어, 느리었, 느린, 느릴, 느림, 느립, 느립니
 * ```
 */
object KoreanConjugation: KLogging() {

    // ㅋ, ㅎ for 잨ㅋㅋㅋㅋ 잔댛ㅎㅎㅎㅎ
    private val CODAS_COMMON = charArrayOf('ㅂ', 'ㅆ', 'ㄹ', 'ㄴ', 'ㅁ')

    // 파랗다 -> 파래, 파램, 파랠, 파랬
    private val CODAS_FOR_CONTRACTION = charArrayOf('ㅆ', 'ㄹ', 'ㅁ')
    private val CODAS_NO_PAST = charArrayOf('ㅂ', 'ㄹ', 'ㄴ', 'ㅁ')

    private val CODAS_SLANG_CONSONANT = charArrayOf('ㅋ', 'ㅎ')
    private val CODAS_SLANG_VOWEL = charArrayOf('ㅜ', 'ㅠ')

    private val PRE_EOMI_COMMON = "게겠고구기긴길네다더던도든면자잖재져죠지진질".toCharArray()
    private val PRE_EOMI_1_1 = "야서써도준".toCharArray()
    private val PRE_EOMI_1_2 = "어었".toCharArray()
    private val PRE_EOMI_1_3 = "아았".toCharArray()
    private val PRE_EOMI_1_4 = "워웠".toCharArray()
    private val PRE_EOMI_1_5 = "여였".toCharArray()

    private val PRE_EOMI_2 = "노느니냐".toCharArray()
    private val PRE_EOMI_3 = "러려며".toCharArray()
    private val PRE_EOMI_4 = "으".toCharArray()
    private val PRE_EOMI_5 = "은".toCharArray()
    private val PRE_EOMI_6 = "는".toCharArray()
    private val PRE_EOMI_7 = "운".toCharArray()

    // 존대어
    private val PRE_EOMI_RESPECT = "세시실신셔습셨십".toCharArray()

    private val PRE_EOMI_VOWEL: CharArray = PRE_EOMI_COMMON + PRE_EOMI_2 + PRE_EOMI_3 + PRE_EOMI_RESPECT

    private fun addPreEomi(lastChar: Char, charsToAdd: CharArray): List<String> {
        return charsToAdd.map { lastChar + it.toString() }.toList()
    }

    /**
     *
     */
    fun conjugatePredicatesToCharArraySet(words: Set<String>, isAdjective: Boolean = false): CharArraySet {
        val newSet = DictionaryProvider.newCharArraySet()
        newSet.addAll(conjugatePredicated(words, isAdjective))
        return newSet
    }

    private val PRE_EOMI_하다: CharArray by unsafeLazy { PRE_EOMI_COMMON + PRE_EOMI_2 + PRE_EOMI_6 + PRE_EOMI_RESPECT }
    private val PRE_EOMI_VOWEL_하다: CharArray by unsafeLazy { PRE_EOMI_VOWEL + PRE_EOMI_1_5 + PRE_EOMI_6 }

    private val adjective_하다_Set = setOf("합", "해", "히", "하")
    private val adjective_하다_Set2 = setOf("합", "해")

    /**
     * Cases without codas
     * 하다, special case
     */
    private fun expandChar_하다(lastChar: Char, isAdjective: Boolean): List<String> {
        val endings = if (isAdjective) adjective_하다_Set else adjective_하다_Set2
        val preEomi1 = addPreEomi(lastChar, PRE_EOMI_하다)
        val preEomi2 = CODAS_COMMON.map {
            when (it) {
                'ㅆ'  -> composeHangul('ㅎ', 'ㅐ', it).toString()
                else -> composeHangul('ㅎ', 'ㅏ', it).toString()
            }
        }
        val preEomi3 = addPreEomi('하', PRE_EOMI_VOWEL_하다)
        val preEomi4 = addPreEomi('해', PRE_EOMI_1_1)

        return preEomi1 + preEomi2 + preEomi3 + preEomi4 + endings
    }

    private val PRE_EOMI_쏘다: CharArray by lazy { PRE_EOMI_VOWEL + PRE_EOMI_2 + PRE_EOMI_1_3 + PRE_EOMI_6 }

    /**
     * 쏘다
     */
    private fun expandChar_쏘다(lastChar: Char, onset: Char): List<String> {
        return addPreEomi(lastChar, PRE_EOMI_쏘다) +
                CODAS_NO_PAST.map { composeHangul(onset, 'ㅗ', it).toString() }.toList() +
                mutableListOf(
                    composeHangul(onset, 'ㅘ').toString(),
                    composeHangul(onset, 'ㅘ', 'ㅆ').toString(),
                    lastChar.toString()
                )
    }

    private val PRE_EOMI_겨누다 by lazy { PRE_EOMI_VOWEL + PRE_EOMI_1_2 + PRE_EOMI_2 + PRE_EOMI_6 }

    /**
     * 맞추다, 겨누다, 재우다
     */
    private fun expandChar_겨누다(lastChar: Char, onset: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(lastChar, PRE_EOMI_겨누다))
            addAll(CODAS_NO_PAST.map { composeHangul(onset, 'ㅜ', it).toString() })

            add(composeHangul(onset, 'ㅝ').toString())
            add(composeHangul(onset, 'ㅝ', 'ㅆ').toString())
            add(lastChar.toString())
        }
    }

    private val PRE_EOMI_UNION_2_6 by lazy { PRE_EOMI_2 + PRE_EOMI_6 }

    private fun expandChar_치르다(lastChar: Char, onset: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(lastChar, PRE_EOMI_2 + PRE_EOMI_6))
            addAll(CODAS_NO_PAST.map { composeHangul(onset, 'ㅡ', it).toString() })
            addAll(
                listOf(
                    composeHangul(onset, 'ㅝ').toString(),
                    composeHangul(onset, 'ㅓ').toString(),
                    composeHangul(onset, 'ㅏ').toString(),
                    composeHangul(onset, 'ㅝ', 'ㅆ').toString(),
                    composeHangul(onset, 'ㅓ', 'ㅆ').toString(),
                    composeHangul(onset, 'ㅏ', 'ㅆ').toString(),
                    lastChar.toString()
                )
            )
        }
    }

    private fun expandChar_사귀다(lastChar: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(lastChar, PRE_EOMI_UNION_2_6))
            addAll(CODAS_NO_PAST.map { composeHangul('ㄱ', 'ㅟ', it).toString() })
            add(composeHangul('ㄱ', 'ㅕ', ' ').toString())
            add(composeHangul('ㄱ', 'ㅕ', 'ㅆ').toString())
            add(lastChar.toString())
        }
    }

    private fun expandChar_쥐다(lastChar: Char, onset: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(CODAS_NO_PAST.map { composeHangul(onset, 'ㅟ', it).toString() })
            addAll(addPreEomi(lastChar, PRE_EOMI_UNION_2_6))
            add(lastChar.toString())
        }

    }

    /**
     * 마시다, 엎드리다, 치다, 이다, 아니다
     */
    private fun expandChar_마시다(lastChar: Char, onset: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(CODAS_NO_PAST.map { composeHangul(onset, 'ㅣ', it).toString() })
            addAll(addPreEomi(lastChar, PRE_EOMI_1_2 + PRE_EOMI_UNION_2_6))
            add(composeHangul(onset, 'ㅣ', 'ㅂ') + "니")
            add(composeHangul(onset, 'ㅕ').toString())
            add(composeHangul(onset, 'ㅕ', 'ㅆ').toString())
            add(lastChar.toString())
        }
    }

    /**
     * 꿰다, 꾀다
     */
    private fun expandChar_꿰다(lastChar: Char, onset: Char, vowel: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(lastChar, PRE_EOMI_UNION_2_6))
            addAll(CODAS_COMMON.map { composeHangul(onset, vowel, it).toString() })
            add(lastChar.toString())
        }
    }

    private val PRE_EOMI_물러서다 by lazy { PRE_EOMI_VOWEL + PRE_EOMI_1_1 + PRE_EOMI_2 + PRE_EOMI_6 }

    /**
     * 나머지 받침없는 서술어 (둘러서다, 켜다, 세다, 캐다, 차다)
     */
    private fun expandChar_물러서다(lastChar: Char, onset: Char, vowel: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(CODAS_COMMON.map { composeHangul(onset, vowel, it).toString() })
            addAll(addPreEomi(lastChar, PRE_EOMI_물러서다))
            add(lastChar.toString())
        }
    }

    private val PRE_EOMI_만들다 by lazy { PRE_EOMI_1_2 + PRE_EOMI_3 }
    private val PRE_EOMI_만들다_2 by lazy { PRE_EOMI_2 + PRE_EOMI_6 + PRE_EOMI_RESPECT }

    /**
     * Cases with codas : 만들다, 알다, 풀다
     */
    private fun expandChar_만들다(lastChar: Char, onset: Char, vowel: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(lastChar, PRE_EOMI_만들다))
            addAll(addPreEomi(composeHangul(onset, vowel), PRE_EOMI_만들다_2))
            addAll(
                mutableListOf(
                    composeHangul(onset, vowel, 'ㄻ').toString(),
                    composeHangul(onset, vowel, 'ㄴ').toString(),
                    lastChar.toString()
                )
            )
        }
    }

    private val PRE_EOMI_UNION_4_5 by lazy { PRE_EOMI_4 + PRE_EOMI_5 }

    private fun expandChar_낫다(lastChar: Char, onset: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(lastChar, PRE_EOMI_UNION_2_6))
            addAll(addPreEomi(composeHangul(onset, 'ㅏ'), PRE_EOMI_UNION_4_5))
            add(lastChar.toString())
        }
    }

    private val PRE_EOMI_붇다 by lazy { PRE_EOMI_1_2 + PRE_EOMI_1_4 + PRE_EOMI_4 + PRE_EOMI_5 }

    private fun expandChar_붇다(lastChar: Char, onset: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(lastChar, PRE_EOMI_UNION_2_6))
            addAll(addPreEomi(composeHangul(onset, 'ㅜ'), PRE_EOMI_붇다))
            add(composeHangul(onset, 'ㅜ', 'ㄹ').toString())
            add(lastChar.toString())
        }
    }

    private val PRE_EOMI_눕다 by lazy { PRE_EOMI_1_4 + PRE_EOMI_4 + PRE_EOMI_5 }

    private fun expandChar_눕다(lastChar: Char, onset: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(lastChar, PRE_EOMI_UNION_2_6))
            addAll(addPreEomi(composeHangul(onset, 'ㅜ'), PRE_EOMI_눕다))
            add(lastChar.toString())
        }
    }

    private val PRE_EOMI_UNION_1_4_7 by lazy { PRE_EOMI_1_4 + PRE_EOMI_7 }

    /**
     *  간지럽다, 갑작스럽다 -> 갑작스런
     */
    private fun expandChar_간지럽다(lastChar: Char, onset: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(composeHangul(onset, 'ㅓ'), PRE_EOMI_UNION_1_4_7))
            add(composeHangul(onset, 'ㅓ').toString())
            add(composeHangul(onset, 'ㅓ', 'ㄴ').toString())
            add(lastChar.toString())
        }
    }

    private fun expandChar_아름답다(lastChar: Char, onset: Char, vowel: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(composeHangul(onset, vowel), PRE_EOMI_UNION_1_4_7))
            add(composeHangul(onset, vowel).toString())
            add(lastChar.toString())
        }
    }

    private fun expandChar_놓다(lastChar: Char, onset: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(addPreEomi(lastChar, PRE_EOMI_UNION_2_6))
            addAll(CODAS_COMMON.map { composeHangul(onset, 'ㅗ', it).toString() })
            addAll(
                mutableListOf(
                    composeHangul(onset, 'ㅘ').toString(),
                    composeHangul(onset, 'ㅗ').toString(),
                    lastChar.toString()
                )
            )
        }
    }

    /**
     * 파랗다, 퍼렇다, 어떻다
     */
    private fun expandChar_파랗다(lastChar: Char, onset: Char, vowel: Char): List<String> {
        return mutableListOf<String>().apply {
            addAll(CODAS_COMMON.map { composeHangul(onset, vowel, it).toString() })
            addAll(CODAS_FOR_CONTRACTION.map { composeHangul(onset, 'ㅐ', it).toString() })
            addAll(
                listOf(
                    composeHangul(onset, 'ㅐ').toString(),
                    composeHangul(onset, vowel).toString(),
                    lastChar.toString()
                )
            )
        }
    }

    private val PRE_EOMI_있다 by lazy {
        PRE_EOMI_COMMON + PRE_EOMI_1_2 + PRE_EOMI_1_3 + PRE_EOMI_2 + PRE_EOMI_4 + PRE_EOMI_5 + PRE_EOMI_6
    }
    private val PRE_EOMI_밝다 by lazy {
        PRE_EOMI_COMMON + PRE_EOMI_1_2 + PRE_EOMI_1_3 + PRE_EOMI_2 + PRE_EOMI_4 + PRE_EOMI_5
    }

    private val EDGE_CASE = hashSetOf("아니", "입", "입니", "나는")

    private val VOWEL_뀌다 = hashSetOf('ㅞ', 'ㅚ', 'ㅙ')

    fun conjugatePredicated(words: Set<String>, isAdjective: Boolean): Set<String> {

        val expanded: Set<String> = words.flatMap { word ->
            val init = word.substring(0, word.length - 1)
            val lastChar = word.last()
            val lastCharString = lastChar.toString()
            val (onset, vowel, coda) = decomposeHangul(lastChar)

            val expandedList: List<String> =
                if (onset == 'ㅎ' && vowel == 'ㅏ' && coda == ' ') {
                    expandChar_하다(lastChar, isAdjective)
                } else if (vowel == 'ㅗ' && coda == ' ') {
                    expandChar_쏘다(lastChar, onset)
                } else if (vowel == 'ㅜ' && coda == ' ') {
                    // 맞추다, 겨누다, 재우다
                    expandChar_겨누다(lastChar, onset)
                } else if (vowel == 'ㅡ' && coda == ' ') {
                    //  치르다, 구르다, 글르다, 뜨다, 모으다, 고르다, 골르다
                    expandChar_치르다(lastChar, onset)
                } else if (onset == 'ㄱ' && vowel == 'ㅟ' && coda == ' ') {
                    expandChar_사귀다(lastChar)
                } else if (vowel == 'ㅟ' && coda == ' ') {
                    expandChar_쥐다(lastChar, onset)
                } else if (vowel == 'ㅣ' && coda == ' ') {
                    expandChar_마시다(lastChar, onset)
                } else if (vowel in `VOWEL_뀌다` && coda == ' ') {
                    expandChar_꿰다(lastChar, onset, vowel)
                } else if (coda == ' ') {
                    // 나머지 받침없는 서술어 (둘러서다, 켜다, 세다, 캐다, 차다)
                    expandChar_물러서다(lastChar, onset, vowel)
                } else if (coda == 'ㄹ' && ((onset == 'ㅁ' && vowel == 'ㅓ') || vowel == 'ㅡ' || vowel == 'ㅏ' || vowel == 'ㅜ')) {
                    // 만들다, 알다, 풀다
                    expandChar_만들다(lastChar, onset, vowel)
                } else if (vowel == 'ㅏ' && coda == 'ㅅ') {
                    // 낫다, 빼앗다
                    expandChar_낫다(lastChar, onset)
                } else if (onset == 'ㅁ' && vowel == 'ㅜ' && coda == 'ㄷ') {
                    // 묻다
                    addPreEomi(lastChar, PRE_EOMI_UNION_2_6) +
                            mutableListOf(composeHangul('ㅁ', 'ㅜ', 'ㄹ').toString(), lastCharString)
                } else if (vowel == 'ㅜ' && coda == 'ㄷ') {
                    expandChar_붇다(lastChar, onset)
                } else if (vowel == 'ㅜ' && coda == 'ㅂ') {
                    expandChar_눕다(lastChar, onset)
                } else if (vowel == 'ㅓ' && coda == 'ㅂ' && isAdjective) {
                    expandChar_간지럽다(lastChar, onset)
                } else if (coda == 'ㅂ' && isAdjective) {
                    expandChar_아름답다(lastChar, onset, vowel)
                } else if (vowel == 'ㅗ' && coda == 'ㅎ') {
                    expandChar_놓다(lastChar, onset)
                } else if (coda == 'ㅎ' && isAdjective) {
                    expandChar_파랗다(lastChar, onset, vowel)
                } else if (word.length == 1 || (isAdjective && coda == 'ㅆ')) {
                    // 1 char with coda adjective, 있다, 컸다
                    addPreEomi(lastChar, PRE_EOMI_있다) + mutableListOf(lastCharString)
                } else if (word.length == 1 && isAdjective) {
                    // 1 char with coda adjective, 밝다
                    addPreEomi(lastChar, PRE_EOMI_밝다) + mutableListOf(lastCharString)
                } else {
                    // 부여잡다, 얻어맞다, 얻어먹다
                    mutableListOf(lastCharString)
                }

            // -르 불규칙 (고르다 -> 골르다)
            val irregularExpression = if (lastChar == '르' && !hasCoda(init.last())) {
                val lastInitCharDecomposed = decomposeHangul(init.last())
                val newInit = init.substring(0, init.length - 1) + composeHangul(
                    lastInitCharDecomposed.onset,
                    lastInitCharDecomposed.vowel,
                    'ㄹ'
                )
                val o = onset
                val conjugation = mutableListOf<String>().apply {
                    addAll(addPreEomi(lastChar, PRE_EOMI_UNION_2_6))
                    addAll(CODAS_NO_PAST.map { composeHangul(o, 'ㅡ', it).toString() })
                    addAll(
                        listOf(
                            composeHangul(o, 'ㅝ').toString(),
                            composeHangul(o, 'ㅓ').toString(),
                            composeHangul(o, 'ㅏ').toString(),
                            composeHangul(o, 'ㅝ', 'ㅆ').toString(),
                            composeHangul(o, 'ㅓ', 'ㅆ').toString(),
                            composeHangul(o, 'ㅏ', 'ㅆ').toString(),
                            lastCharString
                        )
                    )
                }
                conjugation.map { newInit + it }
            } else {
                emptyList()
            }
            expandedList.map { init + it } + irregularExpression
        }.toSet()

        // Edge cases: these more likely to be a conjugation of an adjective than a verb
        return if (isAdjective) expanded else expanded - EDGE_CASE
    }
}
