package io.bluetape4k.io

import java.io.InputStream
import java.io.PushbackInputStream
import java.nio.charset.Charset

private const val BOM_SIZE = 4
private const val ZZ = 0x00.toByte()
private const val EF = 0xEF.toByte()
private const val BB = 0xBB.toByte()
private const val BF = 0xBF.toByte()
private const val FE = 0xFE.toByte()
private const val FF = 0xFF.toByte()

private val utf_32be_array = byteArrayOf(ZZ, ZZ, FE, FF)
private val utf_32le_array = byteArrayOf(FF, FE, ZZ, ZZ)
private val utf_8_array = byteArrayOf(EF, BB, BF)
private val utf_16be_array = byteArrayOf(FE, FF)
private val utf_16le_array = byteArrayOf(FF, FE)

/**
 * 바이트 배열의 앞부분에서 BOM(Byte Order Mark)을 감지하여 건너뛸 바이트 수와 인코딩 문자셋을 반환합니다.
 *
 * 지원하는 BOM 형식:
 * - UTF-32 BE: `00 00 FE FF` (4바이트)
 * - UTF-32 LE: `FF FE 00 00` (4바이트)
 * - UTF-8:     `EF BB BF`    (3바이트)
 * - UTF-16 BE: `FE FF`       (2바이트)
 * - UTF-16 LE: `FF FE`       (2바이트)
 *
 * BOM이 없으면 건너뛸 바이트 수 0과 [defaultCharset]을 반환합니다.
 *
 * @param defaultCharset BOM이 없을 때 사용할 기본 문자셋 (기본값: UTF-8)
 * @return `(건너뛸 바이트 수, 감지된 문자셋)` 쌍
 */
fun ByteArray.getBOM(defaultCharset: Charset = Charsets.UTF_8): Pair<Int, Charset> {
    val bom4 = this.copyOf(BOM_SIZE)
    if (bom4.contentEquals(utf_32be_array))
        return Pair(4, Charsets.UTF_32BE)
    if (bom4.contentEquals(utf_32le_array))
        return Pair(4, Charsets.UTF_32LE)

    val bom3 = bom4.copyOf(3)
    if (bom3.contentEquals(utf_8_array))
        return Pair(3, Charsets.UTF_8)

    val bom2 = bom3.copyOf(2)
    if (bom2.contentEquals(utf_16be_array))
        return Pair(2, Charsets.UTF_16BE)
    if (bom2.contentEquals(utf_16le_array))
        return Pair(2, Charsets.UTF_16LE)

    return Pair(0, defaultCharset)
}

/**
 * 바이트 배열에서 BOM을 제거하고 BOM이 없는 데이터와 감지된 문자셋을 반환합니다.
 *
 * BOM이 없으면 원본 배열과 [defaultCharset]을 그대로 반환합니다.
 *
 * 사용 예:
 * ```kotlin
 * val (data, charset) = rawBytes.removeBom()
 * val text = String(data, charset)
 * ```
 *
 * @param defaultCharset BOM이 없을 때 사용할 기본 문자셋 (기본값: UTF-8)
 * @return `(BOM 제거된 바이트 배열, 감지된 문자셋)` 쌍
 */
fun ByteArray.removeBom(defaultCharset: Charset = Charsets.UTF_8): Pair<ByteArray, Charset> {
    val (skipSize, charset) = getBOM(defaultCharset)
    val array = if (skipSize > 0) this.copyOfRange(skipSize, size) else this

    return array to charset
}

fun InputStream.withoutBom(defaultCharset: Charset = Charsets.UTF_8): Pair<InputStream, Charset> {
    val bom = ByteArray(BOM_SIZE)
    val pushbackStream = PushbackInputStream(this, BOM_SIZE)
    val readSize = pushbackStream.read(bom, 0, bom.size)
    val (skipSize, charset) = bom.getBOM(defaultCharset)

    pushbackStream.unread(bom, skipSize, readSize - skipSize)
    return pushbackStream to charset
}
