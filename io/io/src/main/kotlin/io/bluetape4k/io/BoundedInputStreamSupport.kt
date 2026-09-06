package io.bluetape4k.io

import io.bluetape4k.support.requireZeroOrPositiveNumber
import java.io.IOException
import java.io.InputStream

/**
 * 입력 스트림이 설정한 byte 상한을 초과했음을 나타냅니다.
 *
 * [maxBytes]만 안정된 공개 정보입니다. 예외에는 본문이나 부분 결과를 저장하지 않으며,
 * message의 정확한 문구는 호환성 계약이 아닙니다.
 */
class ByteLimitExceededException(
    val maxBytes: Int,
): IOException("Input stream exceeded the configured byte limit: maxBytes=$maxBytes") {
    init {
        maxBytes.requireZeroOrPositiveNumber("maxBytes")
    }
}

/**
 * 이 스트림을 끝까지 읽되 결과가 [maxBytes]를 넘으면 [ByteLimitExceededException]을 던집니다.
 *
 * 초과 시 부분 결과를 반환하지 않으며 판정을 위해 최대 한 byte만 더 읽습니다. 이 함수는
 * 호출자가 소유한 스트림을 닫지 않습니다. 읽기와 마지막 EOF 확인은 blocking될 수 있으므로
 * timeout과 중단은 호출자가 구성해야 합니다.
 *
 * 결과를 만들 때 실제 본문 크기의 약 두 배와 고정 크기 segment가 일시적으로 필요합니다.
 * [maxBytes]가 [Int.MAX_VALUE]여도 큰 배열을 먼저 할당하지 않지만, 실제로 큰 본문을 읽을 때는
 * 그만큼의 가용 메모리가 필요합니다.
 *
 * @param maxBytes 허용할 최대 byte 수
 * @return 상한 이하인 전체 본문
 * @throws IllegalArgumentException [maxBytes]가 음수인 경우
 * @throws ByteLimitExceededException 본문이 [maxBytes]를 초과한 경우
 * @throws IOException 스트림 읽기에 실패한 경우
 */
fun InputStream.readAllBytes(maxBytes: Int): ByteArray {
    maxBytes.requireZeroOrPositiveNumber("maxBytes")

    val segments = ArrayList<ByteArray>()
    var total = 0

    while (total < maxBytes) {
        val segment = ByteArray(minOf(DEFAULT_BUFFER_SIZE, maxBytes - total))
        var segmentSize = 0

        while (segmentSize < segment.size) {
            val count = read(segment, segmentSize, segment.size - segmentSize)
            when {
                count < 0 -> {
                    if (segmentSize > 0) segments += segment.copyOf(segmentSize)
                    return segments.flattenToByteArray(total)
                }

                count > 0 -> {
                    segmentSize += count
                    total += count
                }

                else -> {
                    val value = read()
                    if (value < 0) {
                        if (segmentSize > 0) segments += segment.copyOf(segmentSize)
                        return segments.flattenToByteArray(total)
                    }
                    segment[segmentSize++] = value.toByte()
                    total++
                }
            }
        }
        segments += segment
    }

    if (read() >= 0) throw ByteLimitExceededException(maxBytes)
    return segments.flattenToByteArray(total)
}

private fun List<ByteArray>.flattenToByteArray(totalSize: Int): ByteArray =
    ByteArray(totalSize).also { result ->
        var offset = 0
        forEach { segment ->
            segment.copyInto(result, destinationOffset = offset)
            offset += segment.size
        }
    }
