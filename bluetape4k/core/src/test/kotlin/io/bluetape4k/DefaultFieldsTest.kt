package io.bluetape4k

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

class DefaultFieldsTest {

    @Test
    fun `기본 상수들이 시스템 기본값과 일치한다`() {
        LibraryName shouldBeEqualTo "bluetape4k"
        DefaultLocale shouldBeEqualTo Locale.getDefault()
        DefaultCharset shouldBeEqualTo StandardCharsets.UTF_8
        DefaultCharsetName shouldBeEqualTo StandardCharsets.UTF_8.name()
        DefaultEncoding shouldBeEqualTo StandardCharsets.UTF_8.name().lowercase()
    }

    @Test
    fun `기본 시간대 상수들이 기대값을 가진다`() {
        val now = Instant.now()
        DefaultZoneId shouldBeEqualTo ZoneId.systemDefault()
        UtcZoneId shouldBeEqualTo ZoneId.of("UTC")
        val expectedOffset = ZoneId.systemDefault().rules.getOffset(now)
        DefaultZoneOffset shouldBeEqualTo expectedOffset
        UtcZoneOffset shouldBeEqualTo ZoneOffset.UTC
    }
}
