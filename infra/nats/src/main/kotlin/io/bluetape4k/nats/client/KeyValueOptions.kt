package io.bluetape4k.nats.client

import io.nats.client.JetStreamOptions
import io.nats.client.KeyValueOptions

/**
 * DSL 블록으로 [KeyValueOptions]를 생성합니다.
 *
 * @param builder [KeyValueOptions.Builder]에 적용할 설정 블록
 * @return [KeyValueOptions] 인스턴스
 */
inline fun keyValueOptions(
    @BuilderInference builder: KeyValueOptions.Builder.() -> Unit,
): KeyValueOptions = KeyValueOptions.builder().apply(builder).build()

/**
 * 기존 [KeyValueOptions]를 기반으로 복사/수정합니다.
 *
 * @param kvo 기존 [KeyValueOptions]
 * @param builder 추가 설정 블록
 * @return [KeyValueOptions] 인스턴스
 */
inline fun keyValueOptions(
    kvo: KeyValueOptions,
    @BuilderInference builder: KeyValueOptions.Builder.() -> Unit,
): KeyValueOptions = KeyValueOptions.builder(kvo).apply(builder).build()

/**
 * [JetStreamOptions]를 포함한 [KeyValueOptions]를 생성합니다.
 *
 * @param jso JetStream 옵션
 * @param builder 추가 설정 블록
 * @return [KeyValueOptions] 인스턴스
 */
inline fun keyValueOptions(
    jso: JetStreamOptions,
    @BuilderInference builder: KeyValueOptions.Builder.() -> Unit,
): KeyValueOptions =
    keyValueOptions {
        this.jetStreamOptions(jso)
        builder()
    }
