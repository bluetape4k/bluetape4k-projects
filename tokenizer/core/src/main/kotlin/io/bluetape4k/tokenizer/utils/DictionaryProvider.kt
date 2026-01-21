package io.bluetape4k.tokenizer.utils

import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.asFlow
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.zip.GZIPInputStream

/**
 * 리소스에 있는 사전 파일을 읽어 제공하는 유틸리티 클래스
 */
object DictionaryProvider: KLogging() {

    private const val SPACE = " "
    private const val TAB = "\t"

    /**
     * [stream]을 라인 단위로 읽어 Sequence로 반환합니다.
     */
    fun readStreamByLine(stream: InputStream): Sequence<String> {
        return InputStreamReader(stream, Charsets.UTF_8)
            .buffered()
            .lineSequence()
            .map { it.trim() }
    }

    /**
     * [path]에 있는 리소스 파일을 읽어 라인 단위로 반환합니다.
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
     * [path]에 있는 파일을 읽어 단어의 빈도 정보를 반환합니다.
     *
     * 단어의 빈도 정보는 형태소 분석기 시에 확률로 판단할 때 사용합니다.
     */
    fun readWordFreqs(path: String): Map<CharSequence, Float> {
        val freqRange = 0 until 6
        val map = ConcurrentHashMap<CharSequence, Float>()

        readFileByLineFromResources(path)
            .filter { it.contains(TAB) }
            .map {
                val elems = it.split(TAB, limit = 2)
                elems[0] to elems[1].slice(freqRange).toFloat()
            }
            .forEach {
                map[it.first] = it.second
            }

        return map
    }

    fun readWordMap(filename: String): Sequence<Pair<String, String>> {
        return readFileByLineFromResources(filename)
            .filter { it.contains(SPACE) }
            .map {
                val words = it.split(SPACE, limit = 2)
                words[0] to words[1]
            }
    }

    fun readWordsAsSequence(filename: String): Sequence<String> {
        return readFileByLineFromResources(filename)
    }

    suspend fun readWordsAsSet(vararg paths: String): MutableSet<String> {
        val set = ConcurrentSkipListSet<String>()

        paths.asFlow()
            .async { path ->
                readFileByLineFromResources(path)
            }
            .collect { words ->
                set.addAll(words)
            }

        return set
    }

    /**
     * [paths]에 있는 파일을 읽어 단어를 [CharArraySet]으로 반환합니다.
     */
    suspend fun readWords(vararg paths: String): CharArraySet {
        val set = newCharArraySet()

        paths.asFlow()
            .async { path ->
                readFileByLineFromResources(path)
            }
            .collect { words ->
                set.addAll(words)
            }

        return set
    }

    fun newCharArraySet(): CharArraySet {
        return CharArraySet(5_000)
    }
}
