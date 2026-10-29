package io.bluetape4k.assertions

import org.junit.jupiter.api.Test

class AssertionFacadeCompatibilityTest {

    @Test
    fun `이전 Java facade owner를 유지한다`() {
        listOf(
            "DateTimesInstantZonedKt",
            "DateTimesLegacyKt",
            "DateTimesLocalKt",
            "DurationAssertionsKt",
        ).forEach { owner ->
            Class.forName("io.bluetape4k.assertions.$owner")
        }
    }
}
