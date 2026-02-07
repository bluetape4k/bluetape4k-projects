package io.bluetape4k.io

import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.charset.Charset

/**
 * [OutputStream]에 [data]를 씁니다.
 *
 * ```
 * val file = File("test.txt")
 * file.outputStream().use { stream ->
 *    stream.write("Hello, World!")
 *    stream.write("안녕, 세계!")
 *    stream.write("こんにちは、世界!")
 * }
 * ```
 * @receiver 쓰기 대상 [OutputStream]
 * @param data 쓰려는 문자열
 * @param cs 문자열의 인코딩 방식 (기본값: UTF-8)
 */
fun OutputStream.write(data: String, cs: Charset = Charsets.UTF_8) {
    Channels.newChannel(this).write(cs.encode(data))
}

/**
 * [OutputStream]에 [data]를 씁니다.
 *
 * ```
 * val file = File("test.txt")
 * file.outputStream().use { stream ->
 *    stream.write(StringBuffer("Hello, World!"))
 *    stream.write(StringBuffer("안녕, 세계!"))
 *    stream.write(StringBuffer("こんにちは、世界!"))
 * }
 * ```
 * @receiver 쓰기 대상 [OutputStream]
 * @param data 쓰려는 [StringBuffer]
 * @param cs 문자열의 인코딩 방식 (기본값: UTF-8)
 */
fun OutputStream.write(data: StringBuffer, cs: Charset = Charsets.UTF_8) {
    write(data.toString(), cs)
}
