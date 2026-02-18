package io.bluetape4k.micrometer.instrument.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import java.time.Duration

/**
 * Micrometer 기반의 Retrofit 메트릭 레코더입니다.
 *
 * Retrofit HTTP 호출의 실행 시간을 Micrometer Timer로 기록하며,
 * 다양한 퍼센타일(50%, 70%, 90%, 95%, 97%, 99%)을 함께 수집합니다.
 *
 * 수집되는 태그:
 * - method: HTTP 메서드 (GET, POST 등)
 * - uri: 요청 URI
 * - base_url: 기본 URL
 * - status_code: HTTP 상태 코드
 * - outcome: 결과 분류 (SUCCESS, CLIENT_ERROR 등)
 * - coroutines: 코루틴 사용 여부
 *
 * @param meterRegistry Micrometer 메트릭 레지스트리
 */
class MicrometerRetrofitMetricsRecorder(
    private val meterRegistry: MeterRegistry,
): MetricsRecorder {
    companion object: KLogging() {
        /** Retrofit 요청 메트릭의 기본 키 */
        const val METRICS_KEY = "retrofit2.requests"

        /** 수집할 퍼센타일 값들 */
        private val PERCENTILES = doubleArrayOf(0.5, 0.7, 0.9, 0.95, 0.97, 0.99)

        private fun asTags(tags: Map<String, String>): List<Tag> = tags.map { Tag.of(it.key, it.value) }
    }

    /**
     * 타이밍 메트릭을 Micrometer Timer로 기록합니다.
     *
     * @param tags 메트릭 태그
     * @param duration 실행 시간
     */
    override fun recordTiming(
        tags: Map<String, String>,
        duration: Duration,
    ) {
        log.debug { "Measure $METRICS_KEY with tags $tags duration ${duration.toMillis()} ms recorded." }

        Timer
            .builder(METRICS_KEY)
            .tags(asTags(tags))
            .publishPercentiles(*PERCENTILES)
            .register(meterRegistry)
            .record(duration)
    }
}
