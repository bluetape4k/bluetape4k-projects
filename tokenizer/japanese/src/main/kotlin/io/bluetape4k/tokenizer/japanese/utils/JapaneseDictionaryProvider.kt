package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 일본어 사전을 제공하는 유틸리티 클래스
 */
object JapaneseDictionaryProvider: KLoggingChannel() {

    const val BASE_PATH = "japanesetext"

    /**
     * 지정된 경로의 사전 파일들을 읽어 [MutableSet]으로 반환합니다.
     *
     * @param paths 사전 파일 경로 (BASE_PATH 기준 상대 경로)
     * @return 사전 단어들의 [MutableSet]
     */
    suspend fun readWordsAsSet(vararg paths: String): MutableSet<String> {
        return DictionaryProvider.readWordsAsSet(*paths.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    /**
     * 지정된 경로의 사전 파일들을 읽어 [CharArraySet]으로 반환합니다.
     *
     * @param paths 사전 파일 경로 (BASE_PATH 기준 상대 경로)
     * @return 사전 단어들의 [CharArraySet]
     */
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
