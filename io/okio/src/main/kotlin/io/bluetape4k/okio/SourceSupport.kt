package io.bluetape4k.okio

import okio.BufferedSource
import okio.Source
import okio.buffer

/**
 * [Source]를 [BufferedSource]로 변환합니다.
 *
 * ```kotlin
 * val buffer = bufferOf("hello")
 * val source: BufferedSource = (buffer as Source).buffered()
 * val text = source.readUtf8()
 * // text == "hello"
 * ```
 */
fun Source.buffered(): BufferedSource = buffer()
