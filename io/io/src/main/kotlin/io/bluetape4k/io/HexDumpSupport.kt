package io.bluetape4k.io

import org.apache.commons.io.HexDump
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * [ByteArray] 정보를 Hex Dump 형식으로 [out] 에 출력합니다.
 *
 * ```
 * val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
 * val dump = bytes.hexDump()
 * println(dump)
 * ```
 *
 * @param offset 시작 오프셋
 * @param out    출력할 [StringBuilder]
 * @param index  시작 인덱스
 * @param length 출력할 길이
 * @return
 */
fun ByteArray.hexDump(
    offset: Long = 0L,
    out: StringBuilder = StringBuilder(),
    index: Int = 0,
    length: Int = size,
): StringBuilder {
    HexDump.dump(this, offset, out, index, length)
    return out
}

/**
 * [ByteArray] 정보를 Hex Dump 형식으로 [out] 에 출력합니다.
 *
 * ```
 * val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
 * val out = ByteArrayOutputStream()
 * bytes.hexDump(out)
 * println(out.toString())
 * ```
 *
 * @param out    출력할 [ByteArrayOutputStream]
 * @param offset 시작 오프셋
 * @param index  시작 인덱스
 */
fun ByteArray.hexDump(out: OutputStream, offset: Long = 0L, index: Int = 0) {
    HexDump.dump(this, offset, out, index)
}
