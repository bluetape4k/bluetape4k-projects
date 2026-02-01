package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.collections.eclipse.toUnifiedMap
import io.bluetape4k.collections.eclipse.unifiedMapOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.publicLazy
import io.bluetape4k.tokenizer.korean.utils.KoreanConjugation.conjugatePredicated
import io.bluetape4k.tokenizer.korean.utils.KoreanConjugation.conjugatePredicatesToCharArraySet
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adverb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Conjunction
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Determiner
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Eomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Exclamation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Modifier
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.PreEomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Suffix
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.VerbPrefix
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * 한글 사전을 제공합니다.
 */
object KoreanDictionaryProvider: KLogging() {

    const val BASE_PATH = "koreantext"

    suspend fun readWordsAsSet(vararg filenames: String): MutableSet<String> {
        return DictionaryProvider.readWordsAsSet(paths = filenames.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    suspend fun readWords(vararg filenames: String): CharArraySet {
        return DictionaryProvider.readWords(paths = filenames.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    val koreanEntityFreq: Map<CharSequence, Float> by lazy {
        runBlocking(Dispatchers.IO) {
            DictionaryProvider.readWordFreqs("$BASE_PATH/freq/entity-freq.txt.gz")
        }
    }

    fun addWordsToDictionary(pos: KoreanPos, words: Collection<String>) {
        koreanDictionary[pos]?.addAll(words)
    }

    fun addWordsToDictionary(pos: KoreanPos, vararg words: String) {
        if (words.isNotEmpty()) {
            koreanDictionary[pos]?.addAll(words)
        }
    }

    /**
     * 품사별 한글 사전입니다.
     */
    val koreanDictionary: MutableMap<KoreanPos, CharArraySet> by lazy {
        runBlocking(Dispatchers.IO) {
            unifiedMapOf<KoreanPos, CharArraySet>()
                .apply {
                    put(
                        Noun,
                        readWords(
                            "noun/nouns.txt",
                            "noun/entities.txt",
                            "noun/spam.txt",
                            "noun/names.txt",
                            "noun/twitter.txt",
                            "noun/lol.txt",
                            "noun/slangs.txt",
                            "noun/company_names.txt",
                            "noun/foreign.txt",
                            "noun/geolocations.txt",
                            "noun/profane.txt",
                            "substantives/given_names.txt",
                            "noun/kpop.txt",
                            "noun/bible.txt",
                            "noun/pokemon.txt",
                            "noun/congress.txt",
                            "noun/wikipedia_title_nouns.txt",
                            "noun/brand.txt",
                            "noun/fashion.txt",
                            "noun/commerce.txt",
                            "noun/neologism.txt",
                        )
                    )

                    val verbs = async { readWordsAsSet("verb/verb.txt") }
                    val adjective = async { readWordsAsSet("adjective/adjective.txt") }
                    val adveb = async { readWords("adverb/adverb.txt") }
                    val determiner = async { readWords("auxiliary/determiner.txt") }
                    val exclamation = async { readWords("auxiliary/exclamation.txt") }
                    val josa = async { readWords("josa/josa.txt") }
                    val eomi = async { readWords("verb/eomi.txt") }
                    val preEomi = async { readWords("verb/pre_eomi.txt") }
                    val conjuction = async { readWords("auxiliary/conjunctions.txt") }
                    val modifier = async { readWords("substantives/modifier.txt") }
                    val verbPrefix = async { readWords("verb/verb_prefix.txt") }
                    val suffix = async { readWords("substantives/suffix.txt") }

                    put(Verb, conjugatePredicatesToCharArraySet(verbs.await()))
                    put(Adjective, conjugatePredicatesToCharArraySet(adjective.await(), true))
                    put(Adverb, adveb.await())
                    put(Determiner, determiner.await())
                    put(Exclamation, exclamation.await())
                    put(Josa, josa.await())
                    put(Eomi, eomi.await())
                    put(PreEomi, preEomi.await())
                    put(Conjunction, conjuction.await())
                    put(Modifier, modifier.await())
                    put(VerbPrefix, verbPrefix.await())
                    put(Suffix, suffix.await())
                }
        }
    }

    /**
     * 스팸 관련 명사 사전입니다. (스팸, 욕설, 비속어, 슬랭)
     */
    val spamNouns by lazy {
        runBlocking(Dispatchers.IO) {
            readWords(
                "noun/spam.txt",
                "noun/profane.txt",
                "noun/slangs.txt",
            )
        }
    }

    /**
     * 금칙어를 심각도에 따라 분류한 Dictionary 입니다.
     */
    val blockWords by publicLazy {
        runBlocking(Dispatchers.IO) {
            val low = async { readWords("block/block_low.txt", "block/block_middle.txt", "block/block_high.txt") }
            val middle = async { readWords("block/block_middle.txt", "block/block_high.txt") }
            val high = async { readWords("block/block_high.txt") }

            mapOf(
                io.bluetape4k.tokenizer.model.Severity.LOW to low.await(),
                io.bluetape4k.tokenizer.model.Severity.MIDDLE to middle.await(),
                io.bluetape4k.tokenizer.model.Severity.HIGH to high.await(),
            )
        }
    }

    /**
     * 명사 사전입니다.
     */
    val properNouns by publicLazy {
        runBlocking(Dispatchers.IO) {
            readWords(
                "noun/entities.txt",
                "noun/names.txt",
                "noun/twitter.txt",
                "noun/lol.txt",
                "noun/company_names.txt",
                "noun/foreign.txt",
                "noun/geolocations.txt",
                "substantives/given_names.txt",
                "noun/kpop.txt",
                "noun/bible.txt",
                "noun/pokemon.txt",
                "noun/congress.txt",
                "noun/wikipedia_title_nouns.txt",
                "noun/brand.txt",
                "noun/fashion.txt",
                "noun/neologism.txt"
            )
        }
    }

    /**
     * 사람 이름, 그룹 이름 등 이름 사전입니다.
     */
    val nameDictionary: Map<String, CharArraySet> by publicLazy {
        runBlocking(Dispatchers.IO) {
            val familyName = async { readWords("substantives/family_names.txt") }
            val givenName = async { readWords("substantives/given_names.txt") }
            val fullName = async { readWords("noun/kpop.txt", "noun/foreign.txt", "noun/names.txt") }
            unifiedMapOf(
                "family_name" to familyName.await(),
                "given_name" to givenName.await(),
                "full_name" to fullName.await()
            )
        }
    }

    /**
     * 길이에 따른 맞춤법 사전
     */
    val typoDictionaryByLength: Map<Int, Map<String, String>> by publicLazy {
        runBlocking(Dispatchers.IO) {
            val grouped = DictionaryProvider.readWordMap("$BASE_PATH/typos/typos.txt")
                .groupBy { it.first.length }
            // val grouped = readWordMap("typos/typos.txt").toList().groupBy { it.first.length }
            val result = unifiedMapOf<Int, Map<String, String>>()

            grouped.forEach { (index, pair) ->
                result[index] = pair.associate { (k, v) -> k to v }
            }

            result
        }
    }

    /**
     * 예상되는 원형 정보
     */
    val predicateStems: Map<KoreanPos, Map<String, String>> by publicLazy {
        fun getConjugationMap(words: Set<String>, isAdjective: Boolean): Map<String, String> {
            return words
                .flatMap { word ->
                    conjugatePredicated(setOf(word), isAdjective).map {
                        //                        if(it.startsWith("가느")) {
                        //                            log.trace { "활용=$it, 원형=${word + "다"}, 형용사=$isAdjective" }
                        //                        }
                        it to word + "다"
                    }
                }
                .toUnifiedMap()
        }

        runBlocking(Dispatchers.IO) {
            val verb = async { readWordsAsSet("verb/verb.txt") }
            val adjective = async { readWordsAsSet("adjective/adjective.txt") }
            unifiedMapOf(
                Verb to getConjugationMap(verb.await(), false),
                Adjective to getConjugationMap(adjective.await(), true)
            )
        }
    }
}
