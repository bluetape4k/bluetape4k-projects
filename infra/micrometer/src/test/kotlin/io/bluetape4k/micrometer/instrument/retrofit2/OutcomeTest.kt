package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OutcomeTest {
    companion object: KLogging()

    @ParameterizedTest
    @CsvSource(
        "100, INFORMATION",
        "101, INFORMATION",
        "200, SUCCESS",
        "201, SUCCESS",
        "204, SUCCESS",
        "301, REDIRECTION",
        "302, REDIRECTION",
        "400, CLIENT_ERROR",
        "404, CLIENT_ERROR",
        "500, SERVER_ERROR",
        "503, SERVER_ERROR",
        "0, UNKNOWN",
        "99, UNKNOWN",
        "600, UNKNOWN",
    )
    fun `fromHttpStatus - 상태 코드별 Outcome 반환`(
        statusCode: Int,
        expectedOutcome: String,
    ) {
        val outcome = Outcome.fromHttpStatus(statusCode)
        outcome.name shouldBeEqualTo expectedOutcome
    }

    @Test
    fun `Outcome 매핑 확인 - 각 카테고리별 대표값`() {
        Outcome.fromHttpStatus(0) shouldBeEqualTo Outcome.UNKNOWN
        Outcome.fromHttpStatus(100) shouldBeEqualTo Outcome.INFORMATION
        Outcome.fromHttpStatus(200) shouldBeEqualTo Outcome.SUCCESS
        Outcome.fromHttpStatus(300) shouldBeEqualTo Outcome.REDIRECTION
        Outcome.fromHttpStatus(400) shouldBeEqualTo Outcome.CLIENT_ERROR
        Outcome.fromHttpStatus(500) shouldBeEqualTo Outcome.SERVER_ERROR
    }
}
