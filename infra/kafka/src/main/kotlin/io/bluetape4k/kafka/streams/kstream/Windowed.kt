@file:JvmMultifileClass
@file:JvmName("KStreamKt")

package io.bluetape4k.kafka.streams.kstream

import org.apache.kafka.streams.kstream.Window
import org.apache.kafka.streams.kstream.Windowed

/**
 * Kafka Streams에서 윈도우된 키를 생성합니다.
 *
 * [Windowed]는 시간 윈도우와 관련된 키를 표현하며,
 * 윈도우 집계 작업에서 사용됩니다.
 *
 * 사용 예시:
 * ```kotlin
 * val timeWindow = TimeWindow.of(Duration.ofMinutes(0), Duration.ofMinutes(5))
 * val windowedKey = windowedOf("user-123", timeWindow)
 * ```
 *
 * @param K 키 타입
 * @param key 원본 키 값
 * @param window 시간 윈도우
 * @return [Windowed] 인스턴스
 */
fun <K> windowedOf(
    key: K,
    window: Window,
): Windowed<K> = Windowed(key, window)
