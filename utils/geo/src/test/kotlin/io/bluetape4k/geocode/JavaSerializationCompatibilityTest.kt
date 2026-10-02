package io.bluetape4k.geocode

import io.bluetape4k.AbstractValueObject
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.geocode.bing.BingAddress
import io.bluetape4k.geocode.google.GoogleAddress
import io.bluetape4k.io.lookup
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.HexFormat

class JavaSerializationCompatibilityTest {

    // 이 fixture들은 공개 `bluetape4k-geo:2.0.0` artifact의 클래스로 생성했습니다.
    // 주소 fixture는 Address와 상속된 AbstractValueObject descriptor도 포함합니다.

    @Test
    fun `2_0_0에서 계산된 serialVersionUID를 유지한다`() {
        Geocode::class.lookup().serialVersionUID shouldBeEqualTo -9090722762707661091L
        Address::class.lookup().serialVersionUID shouldBeEqualTo 7156298922689609881L
        BingAddress::class.lookup().serialVersionUID shouldBeEqualTo -6688773129200423988L
        GoogleAddress::class.lookup().serialVersionUID shouldBeEqualTo -8088611479544946254L
        AbstractValueObject::class.lookup().serialVersionUID shouldBeEqualTo -202736522154801100L
    }

    @Test
    fun `2_0_0 Geocode 스트림을 읽는다`() {
        val actual = readLegacyFixture<Geocode>(
            "/serialization/geocode-v2.0.0.ser",
            "1770dd1fc3839966b073441fe6baef100f445a71882e5d6c1d161a50cb7a052a",
        )

        actual.latitude shouldBeEqualTo BigDecimal("37.5665")
        actual.longitude shouldBeEqualTo BigDecimal("126.9780")
    }

    @Test
    fun `2_0_0 Bing 주소 스트림을 읽는다`() {
        val actual = readLegacyFixture<BingAddress>(
            "/serialization/bing-address-v2.0.0.ser",
            "6e5504eb75f11cc779e0caee7f132d5574c35ad56f744b5c4472b5420f76a0bf",
        )

        actual.name shouldBeEqualTo "Gangnam"
        actual.country shouldBeEqualTo "Korea"
        actual.city shouldBeEqualTo "Seoul"
        actual.detailAddress shouldBeEqualTo "Teheran-ro 123"
        actual.zipCode shouldBeEqualTo "06234"
        actual.formattedAddress shouldBeEqualTo "Gangnam-gu, Seoul"
    }

    @Test
    fun `2_0_0 Google 주소 스트림을 읽는다`() {
        val actual = readLegacyFixture<GoogleAddress>(
            "/serialization/google-address-v2.0.0.ser",
            "8a97a71796a18a22218ea3efa5bfb5139f73280d75a799247f9c4ca278a38825",
        )

        actual.placeId shouldBeEqualTo "place-123"
        actual.country shouldBeEqualTo "Korea"
        actual.city shouldBeEqualTo "Seoul"
        actual.detailAddress shouldBeEqualTo "Teheran-ro 123"
        actual.zipCode shouldBeEqualTo "06234"
        actual.formattedAddress shouldBeEqualTo "Gangnam-gu, Seoul"
    }

    private inline fun <reified T: Any> readLegacyFixture(path: String, expectedSha256: String): T {
        val bytes = checkNotNull(javaClass.getResourceAsStream(path)) { "Fixture not found: $path" }
            .use { it.readBytes() }
        val actualSha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
        actualSha256 shouldBeEqualTo expectedSha256

        return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as T }
    }
}
