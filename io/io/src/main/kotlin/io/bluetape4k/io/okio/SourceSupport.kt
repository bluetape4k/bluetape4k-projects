package io.bluetape4k.io.okio

import okio.BufferedSource
import okio.Source
import okio.buffer

/**
 * [Source]를 [BufferedSource]로 변환합니다.
 */
fun Source.buffered(): BufferedSource = buffer()
