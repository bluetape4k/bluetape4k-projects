package io.bluetape4k.io.digest

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class FingerprintEncoderTest {

    @Test
    fun `domain separated sha256 uses nul framed fields`() {
        FingerprintEncoder.domainSeparatedSha256Hex("reservation-v1", "tenant", "reserve", "key") shouldBeEqualTo
            "7b0e1c9eaa67130872ac62abdfae41001396f11514c6f53032784d55a34e1e71"
    }

    @Test
    fun `hmac sha256 is url safe and unpadded`() {
        FingerprintEncoder.hmacSha256Base64Url(
            secret = ByteArray(32) { it.toByte() },
            domain = "ticket-v1",
            "raw-key",
        ) shouldBeEqualTo "0DPBde3KI1tberh4fNEYEonZoV4j3isTK-n0EtI_EeU"
    }

    @Test
    fun `raw sha256 keeps the upstream digest output`() {
        FingerprintEncoder.sha256Hex("payload") shouldBeEqualTo
            "239f59ed55e737c77147cf55ad0c1b030b6d7ee748a7426952f9b852d5a935e5"
    }

    @Test
    fun `raw sha256 can preserve url safe base64 output`() {
        FingerprintEncoder.sha256Base64Url("payload") shouldBeEqualTo
            "I59Z7VXnN8dxR89VrQwbAwttfudIp0JpUvm4UtWpNeU"
    }

    @Test
    fun `empty secret is rejected`() {
        val failure = assertFailsWith<IllegalArgumentException> {
            FingerprintEncoder.hmacSha256Base64Url(ByteArray(0), "domain")
        }

        failure.message.shouldNotBeNull() shouldContain "secret"
    }
}
