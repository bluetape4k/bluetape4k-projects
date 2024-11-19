package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 일본어 사전을 제공하는 유틸리티 클래스
 */
object JapaneseDictionaryProvider: KLogging() {

    const val BASE_PATH = "japanesetext"

    suspend fun readWordsAsSet(vararg paths: String): MutableSet<String> {
        return DictionaryProvider.readWordsAsSet(*paths.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    suspend fun readWords(vararg paths: String): CharArraySet {
        return DictionaryProvider.readWords(*paths.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    /**
     * 금칙어 사전
     */
    val blockWordDictionary: CharArraySet by lazy {
        runBlocking(Dispatchers.IO) {
            readWords("block/blocks.txt")
        }
    }

    /**
     * 금칙어를 사전에 추가합니다.
     *
     * @param words 추가할 금칙어
     */
    fun addBlockwords(words: Collection<String>) {
        log.debug { "Add block words: ${words.joinToString(",")}" }
        blockWordDictionary.addAll(words)
    }

    /**
     * 사전에서 해당 금칙어를 삭제합니다.
     *
     * @param words 삭제할 금칙어
     */
    fun removeBlockwords(words: Collection<String>) {
        log.debug { "Remove block words: ${words.joinToString(",")}" }
        blockWordDictionary.removeAll(words)
    }

    /**
     * 모든 금칙어를 삭제합니다.
     */
    fun clearBlockwords() {
        log.debug { "Clear block words" }
        blockWordDictionary.clear()
    }
}
