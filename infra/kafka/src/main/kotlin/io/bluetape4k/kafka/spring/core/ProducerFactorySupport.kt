package io.bluetape4k.kafka.spring.core

import org.springframework.kafka.core.KafkaResourceHolder
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.core.ProducerFactoryUtils
import java.time.Duration

/**
 * 현 트랜잭션과 동기화된 Kafka 리소스를 얻습니다.
 *
 * @param producerFactory ProducerFactory instance.
 * @param <K> 키 타입
 * @param <V> 값 타입
 * @return [KafkaResourceHolder] instance.
 */
fun <K, V> transactionalResourceHolderOf(
    producerFactory: ProducerFactory<K, V>,
): KafkaResourceHolder<K, V> =
    ProducerFactoryUtils.getTransactionalResourceHolder(producerFactory)


/**
 * 현 트랜잭션과 동기화된 Kafka 리소스를 얻습니다.
 *
 * @param producerFactory ProducerFactory instance.
 * @param closeTimeout Producer 종료 시간.
 * @param txIdPrefix 트랜잭션 ID prefix.
 * @return [KafkaResourceHolder] instance.
 */
fun <K, V> transactionalResourceHolderOf(
    producerFactory: ProducerFactory<K, V>,
    closeTimeout: Duration,
    txIdPrefix: String? = null,
): KafkaResourceHolder<K, V> =
    ProducerFactoryUtils.getTransactionalResourceHolder(producerFactory, txIdPrefix, closeTimeout)

/**
 * Kafka Resource 를 해제합니다.
 */
fun <K, V> KafkaResourceHolder<K, V>.release() {
    ProducerFactoryUtils.releaseResources(this)
}
