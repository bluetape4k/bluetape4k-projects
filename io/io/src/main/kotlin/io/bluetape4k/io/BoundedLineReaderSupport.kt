@file:JvmName("BoundedLineReaderSupport")

package io.bluetape4k.io

import io.bluetape4k.support.requirePositiveNumber
import io.bluetape4k.support.requireZeroOrPositiveNumber
import java.io.IOException
import java.io.Reader

/**
 * Reader가 반환한 한 줄이 허용한 UTF-16 code unit 상한을 초과했음을 나타냅니다.
 *
 * 예외에는 부분 줄을 저장하지 않습니다. 분기할 때는 [maxLineChars]만 사용하고
 * message의 정확한 문구나 입력 내용을 계약으로 사용하지 마세요.
 *
 * @property maxLineChars 초과가 발생한 줄에 적용한 UTF-16 code unit 상한
 */
class LineLimitExceededException(
    val maxLineChars: Int,
) : IOException("Reader line exceeded the configured character limit: maxLineChars=$maxLineChars") {
    init {
        maxLineChars.requireZeroOrPositiveNumber("maxLineChars")
    }
}

/**
 * Reader에서 한 줄을 읽을 때 UTF-16 code unit 상한을 적용합니다.
 *
 * 한 줄의 길이가 [maxLineChars]를 넘으면 초과한 첫 code unit을 읽는 즉시
 * [LineLimitExceededException]을 발생시킵니다. LF, CRLF, CR을 줄 종결자로 인식하며
 * 종결자는 상한에 포함하지 않습니다. 상한 단위는 UTF-8 byte나 Unicode code point가
 * 아니므로 supplementary character 하나는 두 code unit으로 계산합니다.
 *
 * 내부 buffer는 [bufferSize]로 고정하고, 현재 줄의 허용량을 넘는 bulk read를 요청하지
 * 않습니다. CR 뒤의 LF를 확인할 때만 다음 code unit 하나를 미리 읽을 수 있습니다.
 * 이 wrapper는 원본 [Reader]를 닫지 않으므로 성공·EOF·실패 후의 close는 호출자가
 * 소유합니다. 읽기는 blocking일 수 있으므로 timeout과 coroutine dispatcher도 호출자가
 * 구성해야 합니다.
 *
 * ```kotlin
 * reader.use {
 *     val lines = it.boundedLineReader(maxLineChars = 64 * 1024)
 *     while (true) {
 *         val line = lines.readLine() ?: break
 *         consume(line)
 *     }
 * }
 * ```
 *
 * @param reader 줄을 제공할 Reader
 * @param maxLineChars 허용할 최대 UTF-16 code unit 수
 * @param bufferSize 내부 char buffer 크기
 */
class BoundedLineReader(
    private val reader: Reader,
    maxLineChars: Int,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
) {
    private val maxLineChars = maxLineChars.requireZeroOrPositiveNumber("maxLineChars")
    private val bufferSize = bufferSize.requirePositiveNumber("bufferSize")
    private val buffer = CharArray(this.bufferSize)
    private var bufferIndex = 0
    private var bufferLimit = 0
    private var pendingChar = NO_PENDING_CHAR

    /**
     * 다음 줄을 반환합니다.
     *
     * 입력이 시작부터 EOF이면 `null`을 반환합니다. 빈 줄은 빈 문자열로 반환하고,
     * 마지막 줄에 종결자가 없어도 읽은 code unit을 반환한 뒤 다음 호출에서 `null`을
     * 반환합니다. 초과한 줄은 부분 문자열 없이 [LineLimitExceededException]으로
     * 실패합니다. 이 함수는 원본 Reader를 닫지 않습니다.
     *
     * @return 다음 줄 또는 입력이 끝난 경우 `null`
     * @throws LineLimitExceededException 줄이 [maxLineChars]를 초과한 경우
     * @throws IOException 원본 Reader가 읽기에 실패한 경우
     */
    @Throws(IOException::class)
    fun readLine(): String? {
        val line = StringBuilder(minOf(maxLineChars, bufferSize))
        var lineLength = 0
        var completed = false
        var result: String? = null

        while (!completed) {
            val value = nextChar(lineLength)
            when {
                value < 0 -> {
                    result = if (lineLength == 0) null else line.toString()
                    completed = true
                }
                value == LF -> {
                    result = line.toString()
                    completed = true
                }
                value == CR -> {
                    val following = readAfterCarriageReturn()
                    if (following >= 0 && following != LF) {
                        pendingChar = following
                    }
                    result = line.toString()
                    completed = true
                }
                lineLength == maxLineChars -> throw LineLimitExceededException(maxLineChars)
                else -> {
                    line.append(value.toChar())
                    lineLength++
                }
            }
        }
        return result
    }

    private fun nextChar(lineLength: Int): Int {
        val result = when {
            pendingChar != NO_PENDING_CHAR -> pendingChar.also { pendingChar = NO_PENDING_CHAR }
            bufferIndex < bufferLimit -> buffer[bufferIndex++].code
            else -> {
                val requested = minOf(
                    bufferSize.toLong(),
                    maxLineChars.toLong() - lineLength.toLong() + 1L,
                ).toInt()
                val count = reader.read(buffer, 0, requested)
                when {
                    count > 0 -> {
                        bufferIndex = 1
                        bufferLimit = count
                        buffer[0].code
                    }
                    count < 0 -> EOF
                    else -> reader.read()
                }
            }
        }
        return result
    }

    private fun readAfterCarriageReturn(): Int {
        if (bufferIndex < bufferLimit) {
            return buffer[bufferIndex++].code
        }
        return reader.read()
    }

    private companion object {
        const val EOF = -1
        const val LF = '\n'.code
        const val CR = '\r'.code
        const val NO_PENDING_CHAR = -2
    }
}

/**
 * Reader를 UTF-16 code unit 길이 제한을 적용하는 [BoundedLineReader]로 감쌉니다.
 *
 * 반환된 wrapper는 [this]를 닫지 않습니다. wrapper를 다 사용한 뒤 원본 Reader의
 * close를 호출하거나 `use`로 감싸세요.
 *
 * @param maxLineChars 허용할 최대 UTF-16 code unit 수
 * @param bufferSize 내부 char buffer 크기
 * @return 길이 제한을 적용한 line reader
 */
fun Reader.boundedLineReader(
    maxLineChars: Int,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): BoundedLineReader = BoundedLineReader(this, maxLineChars, bufferSize)
