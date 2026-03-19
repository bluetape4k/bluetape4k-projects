package io.bluetape4k.tokenizer.utils

import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.asFlow
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

/**
 * 클래스패스 리소스 사전 파일을 읽어 토크나이저 입력 구조로 변환하는 유틸리티다.
 *
 * ## 동작/계약
 * - UTF-8 기준으로 라인을 읽고 `trim()` 처리한 문자열을 제공한다.
 * - `.gz` 확장자는 `GZIPInputStream`으로 자동 해제한다.
 * - Flow 기반 비동기 로딩 API를 통해 여러 사전 경로를 병렬 수집할 수 있다.
 *
 * ```kotlin
 * val words = DictionaryProvider.readWordsAsSequence("dict/custom.txt").take(2).toList()
 * // words.size <= 2
 * ```
 */
object DictionaryProvider: KLogging() {

    private const val SPACE = " "
    private const val TAB = "\t"

    /**
     * 입력 스트림을 라인 시퀀스로 읽어 공백 제거된 문자열을 반환한다.
     *
     * ## 동작/계약
     * - UTF-8 디코딩 후 `lineSequence()`로 지연 평가 시퀀스를 생성한다.
     * - 각 라인은 `trim()`을 적용한 값으로 변환된다.
     *
     * ```kotlin
     * val bytes = "a\\n b ".byteInputStream()
     * val lines = DictionaryProvider.readStreamByLine(bytes).toList()
     * // lines == listOf("a", "b")
     * ```
     */
    fun readStreamByLine(stream: InputStream): Sequence<String> {
        return InputStreamReader(stream, Charsets.UTF_8)
            .buffered()
            .lineSequence()
            .map { it.trim() }
    }

    /**
     * 클래스패스 리소스를 열어 라인 시퀀스로 반환한다.
     *
     * ## 동작/계약
     * - `classLoader.getResourceAsStream(path)`가 `null`이면 `check` 예외가 발생한다.
     * - `.gz` 파일은 압축 해제 후 라인 단위로 읽는다.
     * - 일반 텍스트 파일은 원본 스트림을 직접 읽는다.
     *
     * ```kotlin
     * val lines = DictionaryProvider.readFileByLineFromResources("dict/words.txt")
     * // lines.first().isNotBlank() == true
     * ```
     */
    fun readFileByLineFromResources(
        path: String,
        classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    ): Sequence<String> {
        log.debug { "Read a file. path=$path" }

        val stream: InputStream? = classLoader.getResourceAsStream(path)
        check(stream != null) { "Can't open file. path=$path" }

        return if (path.endsWith(".gz")) {
            readStreamByLine(GZIPInputStream(stream))
        } else {
            readStreamByLine(stream)
        }
    }

    /**
     * 단어-빈도 사전 파일을 읽어 `단어 -> 빈도(Float)` 맵으로 적재한다.
     *
     * ## 동작/계약
     * - 탭(`\t`)을 포함한 라인만 처리한다.
     * - 탭 뒤 문자열의 앞 6글자를 `Float`로 파싱해 빈도로 저장한다.
     * - 결과는 전달된 `destination`에 누적되며 동일 키는 마지막 값으로 덮어쓴다.
     *
     * ```kotlin
     * val map = DictionaryProvider.readWordFreqs("dict/freqs.txt", mutableMapOf())
     * // map.isNotEmpty() == true
     * ```
     */
    fun readWordFreqs(
        path: String,
        destination: MutableMap<CharSequence, Float> = mutableMapOf(),
    ): Map<CharSequence, Float> {
        val freqRange = 0 until 6
        // val map = ConcurrentHashMap<CharSequence, Float>()

        readFileByLineFromResources(path)
            .filter { it.contains(TAB) }
            .map {
                val elems = it.split(TAB, limit = 2)
                elems[0] to elems[1].slice(freqRange).toFloat()
            }
            .forEach {
                destination[it.first] = it.second
            }

        return destination
    }

    /**
     * 공백으로 구분된 2열 사전 파일을 `(단어, 매핑값)` 시퀀스로 반환한다.
     *
     * ## 동작/계약
     * - 공백이 포함된 라인만 처리한다.
     * - 각 라인은 첫 공백 기준으로 2개 문자열로 분리된다.
     *
     * ```kotlin
     * val pairs = DictionaryProvider.readWordMap("dict/map.txt").take(1).toList()
     * // pairs.size <= 1
     * ```
     */
    fun readWordMap(filename: String): Sequence<Pair<String, String>> {
        return readFileByLineFromResources(filename)
            .filter { it.contains(SPACE) }
            .map {
                val words = it.split(SPACE, limit = 2)
                words[0] to words[1]
            }
    }

    /**
     * 리소스 파일의 라인을 그대로 단어 시퀀스로 반환한다.
     *
     * ## 동작/계약
     * - 내부적으로 `readFileByLineFromResources`를 그대로 위임 호출한다.
     * - 지연 시퀀스이므로 소비 시점에 실제 읽기가 수행된다.
     *
     * ```kotlin
     * val first = DictionaryProvider.readWordsAsSequence("dict/words.txt").first()
     * // first.isNotBlank() == true
     * ```
     */
    fun readWordsAsSequence(filename: String): Sequence<String> {
        return readFileByLineFromResources(filename)
    }

    /**
     * 여러 리소스 파일의 단어를 비동기로 읽어 `MutableSet`에 누적한다.
     *
     * ## 동작/계약
     * - `paths`를 Flow로 순회하며 각 경로를 `async` 확장으로 비동기 변환한다.
     * - 수집된 라인을 `destination`에 `addAll`로 합친다.
     * - 반환값은 누적 결과가 반영된 동일 `destination` 인스턴스다.
     *
     * ```kotlin
     * val words = DictionaryProvider.readWordsAsSet("dict/a.txt", "dict/b.txt")
     * // words.isNotEmpty() == true
     * ```
     */
    suspend fun readWordsAsSet(
        vararg paths: String,
        destination: MutableSet<String> = mutableSetOf(),
    ): MutableSet<String> {
        paths.asFlow()
            .async { path ->
                readFileByLineFromResources(path)
            }
            .collect { words ->
                destination.addAll(words)
            }

        return destination
    }

    /**
     * 여러 리소스 파일의 단어를 읽어 `CharArraySet`으로 누적 반환한다.
     *
     * ## 동작/계약
     * - 각 경로의 라인 시퀀스를 비동기 Flow로 수집한다.
     * - 수집 결과를 `destination.addAll`로 합치며 중복 단어는 집합 특성상 하나만 유지된다.
     * - 반환값은 입력 `destination` 자체다.
     *
     * ```kotlin
     * val set = DictionaryProvider.readWords("dict/stopwords.txt")
     * // set.size > 0
     * ```
     */
    suspend fun readWords(
        vararg paths: String,
        destination: CharArraySet = newCharArraySet(),
    ): CharArraySet {
        paths.asFlow()
            .async { path ->
                readFileByLineFromResources(path)
            }
            .collect { words ->
                destination.addAll(words)
            }

        return destination
    }

    /**
     * 사전 단어 적재용 기본 `CharArraySet`을 생성한다.
     *
     * ## 동작/계약
     * - 초기 용량 5,000으로 집합을 생성한다.
     * - 사전 로딩 시 리해시 빈도를 줄이기 위한 기본값이다.
     *
     * ```kotlin
     * val set = DictionaryProvider.newCharArraySet()
     * // set.isEmpty() == true
     * ```
     */
    fun newCharArraySet(): CharArraySet {
        return CharArraySet(5_000)
    }
}
