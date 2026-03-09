package io.bluetape4k.micrometer.instrument.retrofit2

import io.micrometer.core.instrument.Tag
import java.time.Duration

/**
 * Retrofit 호출 메트릭을 기록하는 함수형 인터페이스입니다.
 *
 * 정렬이 보장된 [Tag] 컬렉션과 실행 시간을 받아 메트릭 레코더가 이를 적절한 형식으로 기록합니다.
 * 구현체는 동일한 태그 집합에 대해 Meter 를 재사용할 수 있으므로, 호출자는 태그 순서를 안정적으로 유지하는 편이 좋습니다.
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
        tags: Iterable<Tag>,
        duration: Duration,
    )
}
