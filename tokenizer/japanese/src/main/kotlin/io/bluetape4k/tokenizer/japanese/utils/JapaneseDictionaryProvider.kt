package io.bluetape4k.tokenizer.japanese.utils

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.tokenizer.utils.CharArraySet
import io.bluetape4k.tokenizer.utils.DictionaryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 일본어 토크나이저에서 사용하는 금칙어 사전 로딩 및 런타임 갱신 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 사전 리소스 기준 경로는 `BASE_PATH`(`japanesetext`)입니다.
 * - 파일 로드는 `DictionaryProvider`에 위임하며 상대 경로 앞에 `BASE_PATH/`를 붙여 호출합니다.
 * - `blockWordDictionary`는 최초 접근 시 파일을 읽어 메모리에 적재한 뒤 재사용합니다.
 *
 * ```kotlin
 * val hasWord = JapaneseDictionaryProvider.blockWordDictionary.contains("性器")
 *
 * // hasWord == true
 * ```
 */
object JapaneseDictionaryProvider: KLoggingChannel() {

    /**
     * 일본어 사전 리소스를 찾을 때 사용하는 기본 루트 경로입니다.
     *
     * ## 동작/계약
     * - `readWordsAsSet`, `readWords`는 전달받은 상대 경로 앞에 이 값을 접두사로 붙입니다.
     * - 리소스 구조가 바뀌면 이 상수와 배포 리소스 경로를 함께 맞춰야 합니다.
     *
     * ```kotlin
     * val basePath = JapaneseDictionaryProvider.BASE_PATH
     *
     * // basePath == "japanesetext"
     * ```
     */
    const val BASE_PATH = "japanesetext"

    /**
     * 지정한 사전 파일들을 읽어 단어 집합(`MutableSet`)으로 반환합니다.
     *
     * ## 동작/계약
     * - 각 경로는 `BASE_PATH` 기준 상대 경로로 해석됩니다.
     * - 중복 단어는 집합 특성상 하나로 합쳐집니다.
     * - `suspend` 함수이며 실제 로딩은 `DictionaryProvider.readWordsAsSet`에 위임합니다.
     *
     * ```kotlin
     * val words = kotlinx.coroutines.runBlocking {
     *     JapaneseDictionaryProvider.readWordsAsSet("block/blocks.txt")
     * }
     *
     * // result == true (words.isNotEmpty())
     * ```
     */
    suspend fun readWordsAsSet(vararg paths: String): MutableSet<String> {
        return DictionaryProvider.readWordsAsSet(*paths.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    /**
     * 지정한 사전 파일들을 읽어 `CharArraySet`으로 반환합니다.
     *
     * ## 동작/계약
     * - 각 경로는 `BASE_PATH` 기준 상대 경로로 해석됩니다.
     * - 반환 타입은 토큰 매칭에 사용하는 `CharArraySet`입니다.
     * - `suspend` 함수이며 실제 로딩은 `DictionaryProvider.readWords`에 위임합니다.
     *
     * ```kotlin
     * val words = kotlinx.coroutines.runBlocking {
     *     JapaneseDictionaryProvider.readWords("block/blocks.txt")
     * }
     *
     * // result == true (words.isNotEmpty())
     * ```
     */
    suspend fun readWords(vararg paths: String): CharArraySet {
        return DictionaryProvider.readWords(*paths.map { "$BASE_PATH/$it" }.toTypedArray())
    }

    /**
     * 금칙어 판정에 사용하는 메모리 내 사전 집합입니다.
     *
     * ## 동작/계약
     * - 최초 접근 시 `runBlocking(Dispatchers.IO)`로 `block/blocks.txt`를 로드합니다.
     * - 이후 동일 인스턴스를 재사용하므로 추가/삭제/비우기 변경이 즉시 반영됩니다.
     * - 테스트 기준으로 "性器"는 포함되고 "한국어"는 포함되지 않습니다.
     *
     * ```kotlin
     * val dictionary = JapaneseDictionaryProvider.blockWordDictionary
     *
     * // dictionary.contains("性器") == true
     * ```
     */
    val blockWordDictionary: CharArraySet by lazy {
        runBlocking(Dispatchers.IO) {
            readWords("block/blocks.txt")
        }
    }

    /**
     * 금칙어 사전에 단어 컬렉션을 추가합니다.
     *
     * ## 동작/계약
     * - 내부 사전은 집합이므로 중복 단어는 한 번만 저장됩니다.
     * - 호출 직후 `findBlockwords`/`maskBlockwords` 판정에 반영됩니다.
     * - 빈 컬렉션 전달 시 변경 없이 종료됩니다.
     *
     * ```kotlin
     * JapaneseDictionaryProvider.addBlockwords(listOf("19禁", "29禁"))
     * val value = JapaneseDictionaryProvider.blockWordDictionary.contains("19禁")
     *
     * // value == true
     * ```
     */
    fun addBlockwords(words: Collection<String>) {
        log.debug { "Add block words: ${words.joinToString(",")}" }
        blockWordDictionary.addAll(words)
    }

    /**
     * 금칙어 사전에서 단어 컬렉션을 제거합니다.
     *
     * ## 동작/계약
     * - 존재하지 않는 단어는 무시되고 예외가 발생하지 않습니다.
     * - 호출 직후 동일 단어는 금칙어 판정에서 제외됩니다.
     * - 빈 컬렉션 전달 시 변경 없이 종료됩니다.
     *
     * ```kotlin
     * JapaneseDictionaryProvider.removeBlockwords(listOf("19禁"))
     * val value = JapaneseDictionaryProvider.blockWordDictionary.contains("19禁")
     *
     * // value == false
     * ```
     */
    fun removeBlockwords(words: Collection<String>) {
        log.debug { "Remove block words: ${words.joinToString(",")}" }
        blockWordDictionary.removeAll(words)
    }

    /**
     * 메모리에 로드된 금칙어 사전을 모두 비웁니다.
     *
     * ## 동작/계약
     * - 현재 프로세스의 `blockWordDictionary`만 비우며 리소스 파일은 변경하지 않습니다.
     * - 이후 재로딩이 필요하면 별도 초기화 사이클(프로세스 재시작 등)이 필요합니다.
     * - 호출 후 `blockWordDictionary.isEmpty()`는 `true`입니다.
     *
     * ```kotlin
     * JapaneseDictionaryProvider.clearBlockwords()
     * val value = JapaneseDictionaryProvider.blockWordDictionary.isEmpty()
     *
     * // value == true
     * ```
     */
    fun clearBlockwords() {
        log.debug { "Clear block words" }
        blockWordDictionary.clear()
    }
}
