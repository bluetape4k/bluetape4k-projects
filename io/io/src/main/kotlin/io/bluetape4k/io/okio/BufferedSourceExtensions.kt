package io.bluetape4k.io.okio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.BufferedSource

/**
 * Okio I/O에서 데이터를 읽어오는 `readUtf8Lines` 함수를 제공합니다.
 */
fun BufferedSource.readUtf8Lines(): Sequence<String> = sequence {
    while (true) {
        val line = readUtf8Line() ?: break
        yield(line)
    }
}

/**
 * Okio I/O에서 데이터를 읽어오는 `readUtf8LinesAsFlow` 함수를 제공합니다.
 */
fun BufferedSource.readUtf8LinesAsFlow(): Flow<String> = flow {
    while (true) {
        val line = readUtf8Line() ?: break
        emit(line)
    }
}
