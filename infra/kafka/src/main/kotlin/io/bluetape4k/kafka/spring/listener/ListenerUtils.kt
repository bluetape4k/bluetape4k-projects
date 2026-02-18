package io.bluetape4k.kafka.spring.listener

import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.springframework.kafka.listener.ListenerType
import org.springframework.kafka.listener.ListenerUtils
import org.springframework.kafka.listener.MessageListenerContainer

/**
 * 리스너 객체로부터 [ListenerType]을 결정합니다.
 *
 * 이 함수는 리스너가 Record Listener인지 Batch Listener인지 등을 판별합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val listenerType = listenerTypeOf(myListener)
 * when (listenerType) {
 *     ListenerType.RECORD -> println("Record listener")
 *     ListenerType.BATCH -> println("Batch listener")
 *     else -> println("Other listener type")
 * }
 * ```
 *
 * @param listener 리스너 객체
 * @return [ListenerType] 리스너 유형
 */
fun listenerTypeOf(listener: Any): ListenerType = ListenerUtils.determineListenerType(listener)

/**
 * 중단 가능한 sleep을 수행합니다.
 *
 * 컨테이너가 중지되면 sleep이 즉시 종료됩니다.
 *
 * 사용 예시:
 * ```kotlin
 * container.stoppableSleep(5000)  // 5초 sleep, 중단 가능
 * ```
 *
 * @param interval sleep 시간 (밀리초)
 */
fun MessageListenerContainer.stoppableSleep(interval: Long) {
    ListenerUtils.stoppableSleep(this, interval)
}

/**
 * 주어진 오프셋으로 [OffsetAndMetadata]를 생성합니다.
 *
 * 컨테이너의 설정에 따라 적절한 메타데이터를 포함한 OffsetAndMetadata를 생성합니다.
 *
 * 사용 예시:
 * ```kotlin
 * val offsetAndMetadata = container.createOffsetAndMetadata(100L)
 * consumer.commitSync(mapOf(topicPartition to offsetAndMetadata))
 * ```
 *
 * @param offset 커밋할 오프셋
 * @return [OffsetAndMetadata] 인스턴스
 */
fun MessageListenerContainer.createOffsetAndMetadata(offset: Long): OffsetAndMetadata =
    ListenerUtils.createOffsetAndMetadata(this, offset)
