package io.bluetape4k.kafka.spring.listener.adapter

import org.springframework.kafka.listener.adapter.AdapterUtils
import org.springframework.kafka.listener.adapter.ConsumerRecordMetadata

fun consumerRecordMetadataFromArray(vararg datas: Any): Any? =
    AdapterUtils.buildConsumerRecordMetadataFromArray(*datas)

fun consumerRecordMetadataOf(data: Any): ConsumerRecordMetadata? =
    AdapterUtils.buildConsumerRecordMetadata(data)
