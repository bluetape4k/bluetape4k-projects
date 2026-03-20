package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.codec.encodeHexString
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.random.Random

class KsuidEdgeCasesTest {
    @Test
    fun `decode should pad when base62 shorter than expected`() {
        val raw = Random.nextBytes(Ksuid.Seconds.TOTAL_BYTES - 1)
        val shortEncoded = BytesBase62.encode(raw)

        val decoded = BytesBase62.decode(shortEncoded, expectedBytes = Ksuid.Seconds.TOTAL_BYTES)
        decoded.size shouldBeEqualTo Ksuid.Seconds.TOTAL_BYTES
    }

    @Test
    fun `timestamp and payload lengths stay invariant`() {
        val ksuid = Ksuid.Seconds.generate()
        val decoded = BytesBase62.decode(ksuid, expectedBytes = Ksuid.Seconds.TOTAL_BYTES)
        decoded.size shouldBeEqualTo Ksuid.Seconds.TOTAL_BYTES

        val timestampHex = decoded.copyOfRange(0, Ksuid.Seconds.TIMESTAMP_LEN).encodeHexString()
        val payloadHex = decoded.copyOfRange(Ksuid.Seconds.TIMESTAMP_LEN, Ksuid.Seconds.TOTAL_BYTES).encodeHexString()

        timestampHex.length shouldBeEqualTo Ksuid.Seconds.TIMESTAMP_LEN * 2
        payloadHex.length shouldBeEqualTo Ksuid.Seconds.PAYLOAD_LEN * 2
    }
}
