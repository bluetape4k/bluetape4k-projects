package io.bluetape4k.micrometer.instrument.retrofit2

import java.time.Duration

/**
 * Retrofit 호출 메트릭을 기록하는 함수형 인터페이스입니다.
 *
 * 태그와 실행 시간을 받아 메트릭 레코더가 이를 적절한 형식으로 기록합니다.
 * 예: Micrometer Timer, 로깅, 외부 모니터링 시스템 등
 *
 * @see MicrometerRetrofitMetricsRecorder
 */
fun interface MetricsRecorder {
    /**
     * 주어진 태그와 실행 시간을 기록합니다.
     *
     * @param tags 메트릭 태그 (method, uri, status_code 등)
     * @param duration 실행 시간
     */
    fun recordTiming(
        tags: Map<String, String>,
        duration: Duration,
    )
}
