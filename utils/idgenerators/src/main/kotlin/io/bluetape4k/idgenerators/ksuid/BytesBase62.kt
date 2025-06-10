package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray

/**
 * [ByteArray] 정보를 Base62로 인코딩/디코딩합니다.
 *
 * ```
 * val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
 * val encoded = BytesBase62.encode(bytes)
 * val decoded = BytesBase62.decode(encoded)
 * ```
 */
internal object BytesBase62: KLogging() {

    /**
     * [data]를 Base62로 인코딩한 문자열을 반환합니다.
     *
     * ```
     * val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04)
     * val encoded = BytesBase62.encode(bytes)
     * ```
     *
     * @param data 인코딩할 데이터
     * @return Base62로 인코딩된 문자열
     */
    fun encode(data: ByteArray): String {
        val input = BitInputStream(data)

        // 아웃풋 문자가 5비트 데이터를 표현하는 경우 최악의 경우를 위해 용량을 예약합니다.
        return buildString(data.size * Byte.SIZE_BITS / 5 + 1) {

            while (input.hasMore()) {
                // 최대 6비트씩 읽습니다.
                val rawBits = input.readBits(6)

                // 경우에 따라 예외적인 처리가 필요하므로 _bits_에는 다음 출력 문자를 형성하는 데 필요한 최종 데이터 표현이 포함됩니다.
                val bits: Int
                if (rawBits and COMPACK_MASK == COMPACK_MASK) {
                    // 6비트 모두 표현할 수 없으므로 최하위 5비트만 추출하고 1비트를 다시 스트림으로 돌려보냅니다.
                    bits = rawBits and MASK_5BITS
                    input.seekBit(-1)
                } else {
                    bits = rawBits
                }
                append(ENCODE_TABLE[bits])
            }
        }
    }

    /**
     * Base62로 인코딩된 문자열을 디코딩하여 [ByteArray]로 반환합니다.
     *
     * ```
     * val encoded = "abc123"
     * val decoded = BytesBase62.decode(encoded)
     * ```
     *
     * @param base62String 디코딩할 Base62로 인코딩된 문자열
     * @return 디코딩된 [ByteArray]
     */
    fun decode(base62String: String): ByteArray {
        if (base62String.isEmpty()) {
            return emptyByteArray
        }
        val length = base62String.length
        val output = BitOutputStream(length * 6)

        val lastCharPos = length - 1
        repeat(length) {
            // 다음 문자를 위해 디코딩 테이블에서 데이터 비트를 얻습니다.
            val bits = decodedBitsForCharacter(base62String[it])

            // 스트림에 쓰기 위해 필요한 비트 수를 결정합니다.
            val bitsCount: Int = when {
                (bits and COMPACK_MASK) == COMPACK_MASK -> 5
                it >= lastCharPos                       -> output.bitsCountUpToByte()
                else                                    -> 6
            }
            output.writeBits(bitsCount, bits)
        }
        return output.toArray()
    }


    /**
     * 이 배열은 6비트 양수 정수 인덱스 값을 RFC 2045의 표 1에 지정된 "Base62 Alphabet" 동등물로 변환합니다.
     *
     * Thanks to "commons" project in ws.apache.org for this code.
     * http://svn.apache.org/repos/asf/webservices/commons/trunk/modules/util/
     */
    private val ENCODE_TABLE = charArrayOf(
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    )

    /**
     * 이 배열은 RFC 2045의 표 1에 지정된 "Base62 Alphabet"에서 가져온 유니코드 문자를 해당하는 6비트 양수 정수로 변환합니다.
     * Base62 알파벳에 없지만 배열 범위 내에 있는 문자는 -1로 변환됩니다.
     *
     *
     * NOTE: 여기에는 Base62 알파벳에 62와 63 값을 나타낼 수 있는 특수 문자가 없으므로 두 값 모두 이 디코딩 테이블에 없습니다.
     *
     * Thanks to "commons" project in ws.apache.org for this code.
     * http://svn.apache.org/repos/asf/webservices/commons/trunk/modules/util/
     */
    private val DECODE_TABLE = byteArrayOf(
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
        -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
        18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27,
        28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43,
        44, 45, 46, 47, 48, 49, 50, 51
    )

    /**
     * 컴팩트 5비트 형태로 쓰여져야 하는 데이터에 대한 특수 마스크
     */
    private const val COMPACK_MASK = 0x1E // 00011110

    /**
     * 데이터의 5비트를 추출하기 위한 마스크
     */
    private const val MASK_5BITS = 0x1F // 0001111

    private fun decodedBitsForCharacter(char: Char): Int {
        val result: Int = DECODE_TABLE[char.code].toInt()
        if (char.code >= DECODE_TABLE.size || result < 0) {
            throw IllegalArgumentException("Wrong Base62 symbol found: $char")
        }
        return result
    }
}
