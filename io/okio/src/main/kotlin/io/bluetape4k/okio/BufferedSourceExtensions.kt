package io.bluetape4k.okio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.BufferedSource

/**
 * [BufferedSource]에서 UTF-8 텍스트를 한 줄씩 읽는 [Sequence]를 반환합니다.
 * 스트림이 소진되면 시퀀스가 종료됩니다.
 *
 * ```kotlin
 * val source = bufferOf("line1\nline2\nline3")
 * val lines = source.readUtf8Lines().toList()
 * // lines == listOf("line1", "line2", "line3")
 * ```
 */
fun BufferedSource.readUtf8Lines(): Sequence<String> = sequence {
    while (true) {
        val line = readUtf8Line() ?: break
        yield(line)
    }
}

/**
 * [BufferedSource]에서 UTF-8 텍스트를 한 줄씩 읽는 [Flow]를 반환합니다.
 * 스트림이 소진되면 Flow가 완료됩니다.
 *
 * ```kotlin
 * val source = bufferOf("line1\nline2\nline3")
 * val lines = source.readUtf8LinesAsFlow().toList()
 * // lines == listOf("line1", "line2", "line3")
 * ```
 */
fun BufferedSource.readUtf8LinesAsFlow(): Flow<String> = flow {
    while (true) {
        val line = readUtf8Line() ?: break
        emit(line)
    }
}
