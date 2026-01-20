package io.bluetape4k.io.okio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okio.BufferedSource

fun BufferedSource.readUtf8Lines(): Sequence<String> = sequence {
    while (true) {
        val line = readUtf8Line() ?: break
        yield(line)
    }
}

fun BufferedSource.readUtf8LinesAsFlow(): Flow<String> = callbackFlow {
    while (true) {
        val line = readUtf8Line() ?: break
        send(line)
    }
}
