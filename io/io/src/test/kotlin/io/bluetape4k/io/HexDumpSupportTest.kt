package io.bluetape4k.io

import io.bluetape4k.io.apache.ApacheByteArrayOutputStream
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class HexDumpSupportTest: AbstractIOTest() {

    companion object: KLogging()

    @Test
    fun `hex dump to string builder`() {
        val testArray = ByteArray(256) { it.toByte() }
        val out = testArray.hexDump().toString().trim()
        println("out")
        println(out)

        out shouldBeEqualTo
                """
                00000000 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F ................
                00000010 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F ................
                00000020 20 21 22 23 24 25 26 27 28 29 2A 2B 2C 2D 2E 2F  !"#${'$'}%&'()*+,-./
                00000030 30 31 32 33 34 35 36 37 38 39 3A 3B 3C 3D 3E 3F 0123456789:;<=>?
                00000040 40 41 42 43 44 45 46 47 48 49 4A 4B 4C 4D 4E 4F @ABCDEFGHIJKLMNO
                00000050 50 51 52 53 54 55 56 57 58 59 5A 5B 5C 5D 5E 5F PQRSTUVWXYZ[\]^_
                00000060 60 61 62 63 64 65 66 67 68 69 6A 6B 6C 6D 6E 6F `abcdefghijklmno
                00000070 70 71 72 73 74 75 76 77 78 79 7A 7B 7C 7D 7E 7F pqrstuvwxyz{|}~.
                00000080 80 81 82 83 84 85 86 87 88 89 8A 8B 8C 8D 8E 8F ................
                00000090 90 91 92 93 94 95 96 97 98 99 9A 9B 9C 9D 9E 9F ................
                000000A0 A0 A1 A2 A3 A4 A5 A6 A7 A8 A9 AA AB AC AD AE AF ................
                000000B0 B0 B1 B2 B3 B4 B5 B6 B7 B8 B9 BA BB BC BD BE BF ................
                000000C0 C0 C1 C2 C3 C4 C5 C6 C7 C8 C9 CA CB CC CD CE CF ................
                000000D0 D0 D1 D2 D3 D4 D5 D6 D7 D8 D9 DA DB DC DD DE DF ................
                000000E0 E0 E1 E2 E3 E4 E5 E6 E7 E8 E9 EA EB EC ED EE EF ................
                000000F0 F0 F1 F2 F3 F4 F5 F6 F7 F8 F9 FA FB FC FD FE FF ................
                """.trimIndent().trim()


        val out2 = testArray.hexDump(offset = 0x10000000, index = 0x28, length = 32).toString().trim()
        println("out2:")
        println(out2)
        out2 shouldBeEqualTo
                """
                10000028 28 29 2A 2B 2C 2D 2E 2F 30 31 32 33 34 35 36 37 ()*+,-./01234567
                10000038 38 39 3A 3B 3C 3D 3E 3F 40 41 42 43 44 45 46 47 89:;<=>?@ABCDEFG
                """.trimIndent().trim()


        val out3 = testArray.hexDump(index = 0x40, length = 24).toString().trim()
        println("out3:")
        println(out3)
        out3 shouldBeEqualTo
                """
                00000040 40 41 42 43 44 45 46 47 48 49 4A 4B 4C 4D 4E 4F @ABCDEFGHIJKLMNO
                00000050 50 51 52 53 54 55 56 57                         PQRSTUVW
                """.trimIndent().trim()


        assertFailsWith<ArrayIndexOutOfBoundsException> {
            testArray.hexDump(offset = 0x10000000, index = -1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            testArray.hexDump(offset = 0x10000000, index = testArray.size)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            testArray.hexDump(offset = 0x10000000, index = 0, length = -1)
        }
        assertFailsWith<ArrayIndexOutOfBoundsException> {
            testArray.hexDump(offset = 0, index = 1)
        }
    }

    @Test
    fun `hex dump to output stream`() {
        val testArray = ByteArray(256) { it.toByte() }

        ApacheByteArrayOutputStream().use { bos ->
            testArray.hexDump(bos)
            val bytes = bos.toByteArray()
            println("bytes size=${bytes.size}")
            bytes.toUtf8String().trim() shouldBeEqualTo
                    """
                00000000 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F ................
                00000010 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F ................
                00000020 20 21 22 23 24 25 26 27 28 29 2A 2B 2C 2D 2E 2F  !"#${'$'}%&'()*+,-./
                00000030 30 31 32 33 34 35 36 37 38 39 3A 3B 3C 3D 3E 3F 0123456789:;<=>?
                00000040 40 41 42 43 44 45 46 47 48 49 4A 4B 4C 4D 4E 4F @ABCDEFGHIJKLMNO
                00000050 50 51 52 53 54 55 56 57 58 59 5A 5B 5C 5D 5E 5F PQRSTUVWXYZ[\]^_
                00000060 60 61 62 63 64 65 66 67 68 69 6A 6B 6C 6D 6E 6F `abcdefghijklmno
                00000070 70 71 72 73 74 75 76 77 78 79 7A 7B 7C 7D 7E 7F pqrstuvwxyz{|}~.
                00000080 80 81 82 83 84 85 86 87 88 89 8A 8B 8C 8D 8E 8F ................
                00000090 90 91 92 93 94 95 96 97 98 99 9A 9B 9C 9D 9E 9F ................
                000000A0 A0 A1 A2 A3 A4 A5 A6 A7 A8 A9 AA AB AC AD AE AF ................
                000000B0 B0 B1 B2 B3 B4 B5 B6 B7 B8 B9 BA BB BC BD BE BF ................
                000000C0 C0 C1 C2 C3 C4 C5 C6 C7 C8 C9 CA CB CC CD CE CF ................
                000000D0 D0 D1 D2 D3 D4 D5 D6 D7 D8 D9 DA DB DC DD DE DF ................
                000000E0 E0 E1 E2 E3 E4 E5 E6 E7 E8 E9 EA EB EC ED EE EF ................
                000000F0 F0 F1 F2 F3 F4 F5 F6 F7 F8 F9 FA FB FC FD FE FF ................
                """.trimIndent().trim()
        }

        ApacheByteArrayOutputStream().use { bos ->
            testArray.hexDump(bos, offset = 0xFF000000, index = 0)
            val bytes = bos.toByteArray()
            val str = bytes.toUtf8String()
            println("bytes size=${bytes.size}")
            println(str)

            str.trim() shouldBeEqualTo
                    """
                FF000000 00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F ................
                FF000010 10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F ................
                FF000020 20 21 22 23 24 25 26 27 28 29 2A 2B 2C 2D 2E 2F  !"#${'$'}%&'()*+,-./
                FF000030 30 31 32 33 34 35 36 37 38 39 3A 3B 3C 3D 3E 3F 0123456789:;<=>?
                FF000040 40 41 42 43 44 45 46 47 48 49 4A 4B 4C 4D 4E 4F @ABCDEFGHIJKLMNO
                FF000050 50 51 52 53 54 55 56 57 58 59 5A 5B 5C 5D 5E 5F PQRSTUVWXYZ[\]^_
                FF000060 60 61 62 63 64 65 66 67 68 69 6A 6B 6C 6D 6E 6F `abcdefghijklmno
                FF000070 70 71 72 73 74 75 76 77 78 79 7A 7B 7C 7D 7E 7F pqrstuvwxyz{|}~.
                FF000080 80 81 82 83 84 85 86 87 88 89 8A 8B 8C 8D 8E 8F ................
                FF000090 90 91 92 93 94 95 96 97 98 99 9A 9B 9C 9D 9E 9F ................
                FF0000A0 A0 A1 A2 A3 A4 A5 A6 A7 A8 A9 AA AB AC AD AE AF ................
                FF0000B0 B0 B1 B2 B3 B4 B5 B6 B7 B8 B9 BA BB BC BD BE BF ................
                FF0000C0 C0 C1 C2 C3 C4 C5 C6 C7 C8 C9 CA CB CC CD CE CF ................
                FF0000D0 D0 D1 D2 D3 D4 D5 D6 D7 D8 D9 DA DB DC DD DE DF ................
                FF0000E0 E0 E1 E2 E3 E4 E5 E6 E7 E8 E9 EA EB EC ED EE EF ................
                FF0000F0 F0 F1 F2 F3 F4 F5 F6 F7 F8 F9 FA FB FC FD FE FF ................
                """.trimIndent().trim()
        }


        ApacheByteArrayOutputStream().use { bos ->
            testArray.hexDump(bos, 0x10000000, 0x81)
            val bytes = bos.toByteArray()
            println("bytes size=${bytes.size}")
            val str = bytes.toUtf8String()
            println(str)

            str.trim() shouldBeEqualTo
                    """
                10000081 81 82 83 84 85 86 87 88 89 8A 8B 8C 8D 8E 8F 90 ................
                10000091 91 92 93 94 95 96 97 98 99 9A 9B 9C 9D 9E 9F A0 ................
                100000A1 A1 A2 A3 A4 A5 A6 A7 A8 A9 AA AB AC AD AE AF B0 ................
                100000B1 B1 B2 B3 B4 B5 B6 B7 B8 B9 BA BB BC BD BE BF C0 ................
                100000C1 C1 C2 C3 C4 C5 C6 C7 C8 C9 CA CB CC CD CE CF D0 ................
                100000D1 D1 D2 D3 D4 D5 D6 D7 D8 D9 DA DB DC DD DE DF E0 ................
                100000E1 E1 E2 E3 E4 E5 E6 E7 E8 E9 EA EB EC ED EE EF F0 ................
                100000F1 F1 F2 F3 F4 F5 F6 F7 F8 F9 FA FB FC FD FE FF    ...............                    
                """.trimIndent().trim()
        }

    }

    @Test
    fun `hex dump with invalid index`() {
        val testArray = ByteArray(256) { it.toByte() }

        assertFailsWith<ArrayIndexOutOfBoundsException> {
            testArray.hexDump(ApacheByteArrayOutputStream(), offset = 0x10000000, index = -1)
        }

        assertFailsWith<ArrayIndexOutOfBoundsException> {
            testArray.hexDump(ApacheByteArrayOutputStream(), offset = 0x10000000, index = testArray.size * 2)
        }
    }
}
