package io.bluetape4k.kafka.spring.support

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.support.KafkaUtils

/**
 * [ProducerRecord]를 포맷된 문자열로 변환합니다.
 */
fun ProducerRecord<*, *>.prettyString(): String = KafkaUtils.format(this)

/**
 * [ConsumerRecord]를 포맷된 문자열로 변환합니다.
 */
fun ConsumerRecord<*, *>.prettyString(): String = KafkaUtils.format(this)
