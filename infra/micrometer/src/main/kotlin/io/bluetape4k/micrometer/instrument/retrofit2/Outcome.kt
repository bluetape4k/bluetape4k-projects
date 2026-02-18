package io.bluetape4k.micrometer.instrument.retrofit2

/**
 * HTTP 응답 결과를 분류하는 Outcome 열거형입니다.
 *
 * HTTP 상태 코드를 기반으로 요청 결과를 카테고리별로 분류하여
 * 메트릭 수집 시 유용하게 사용됩니다.
 *
 * @property code 상태 코드의 첫 번째 자리 (1xx=1, 2xx=2, ...)
 */
enum class Outcome(
    private val code: Int,
) {
    /** 알 수 없는 결과 */
    UNKNOWN(0),

    /** 정보 응답 (1xx) */
    INFORMATION(1),

    /** 성공 응답 (2xx) */
    SUCCESS(2),

    /** 리다이렉션 (3xx) */
    REDIRECTION(3),

    /** 클라이언트 오류 (4xx) */
    CLIENT_ERROR(4),

    /** 서버 오류 (5xx) */
    SERVER_ERROR(5),
    ;

    companion object {
        /**
         * HTTP 상태 코드에 해당하는 Outcome을 반환합니다.
         *
         * @param statusCode HTTP 상태 코드
         * @return 해당하는 [Outcome] 값, 매칭되지 않으면 [UNKNOWN]
         */
        @JvmStatic
        fun fromHttpStatus(statusCode: Int): Outcome =
            Outcome.entries.firstOrNull { it.code == (statusCode / 100) } ?: UNKNOWN
    }
}
