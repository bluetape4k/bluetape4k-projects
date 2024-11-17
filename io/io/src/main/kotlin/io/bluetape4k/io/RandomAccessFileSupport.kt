package io.bluetape4k.io

import org.apache.commons.io.RandomAccessFiles
import java.io.RandomAccessFile
import java.nio.ByteBuffer

/**
 * [RandomAccessFile] 정보를 읽어서 [ByteBuffer]에 씁니다.
 *
 * ```
 * val file = RandomAccessFile("test.txt", "r")
 * val buffer = ByteBuffer.allocate(file.length().toInt())
 * file.readTo(buffer)
 * ```
 *
 *
 * @receiver RandomAccessFile  읽을 파일
 * @param dstBuffer ByteBuffer 쓸 버퍼
 * @param limit Int 쓸 크기
 * @return Int 읽은 크기
 */
fun RandomAccessFile.readTo(dstBuffer: ByteBuffer, limit: Int = dstBuffer.remaining()): Int {
    return when {
        dstBuffer.hasArray() -> {
            val readCount = read(dstBuffer.array(), dstBuffer.arrayOffset() + dstBuffer.position(), limit)
            if (readCount > 0) {
                dstBuffer.position(dstBuffer.position() + readCount)
            }
            readCount
        }

        else                 -> {
            val array = ByteArray(limit)
            val readCount = read(array)

            if (readCount > 0) {
                dstBuffer.put(array)
            }
            readCount
        }
    }
}

/**
 * [RandomAccessFile]의 내용이 [other]와 같은지 확인합니다.
 *
 * ```
 * val file1 = RandomAccessFile("test1.txt", "r")
 * val file2 = RandomAccessFile("test2.txt", "r")
 * val matched = file1.contentEquals(file2)
 * ```
 *
 * @param other 비교할 [RandomAccessFile]
 */
fun RandomAccessFile.contentEquals(other: RandomAccessFile): Boolean {
    return RandomAccessFiles.contentEquals(this, other)
}

/**
 * [RandomAccessFile]을 읽어서 ByteArray로 반환합니다.
 *
 * ```
 * val file = RandomAccessFile("test.txt", "r")
 * val data = file.read(0, file.length().toInt())
 * ```
 *
 * @receiver RandomAccessFile 읽을 파일
 * @param position 읽을 위치 (기본값: 0)
 * @param length 읽을 길이 (기본값: [RandomAccessFile]의 길이)
 * @return ByteArray 읽은 데이터
 */
fun RandomAccessFile.read(position: Long = 0, length: Int = length().toInt()): ByteArray {
    return RandomAccessFiles.read(this, position, length)
}

/**
 * [RandomAccessFile]의 position을 리셋합니다.
 *
 * ```
 * val file = RandomAccessFile("test.txt", "r")
 * file.reset()
 * ```
 *
 * @receiver RandomAccessFile 리셋할 파일
 * @param position 리셋할 위치 (기본값: 0)
 */
fun RandomAccessFile.reset(position: Long = 0) = apply {
    seek(position)
}
