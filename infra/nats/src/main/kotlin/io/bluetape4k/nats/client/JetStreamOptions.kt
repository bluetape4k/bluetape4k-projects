package io.bluetape4k.nats.client

import io.nats.client.JetStreamOptions
import java.time.Duration

/**
 * JetStream 기본 옵션 인스턴스입니다.
 */
@JvmField
val defaultJetStreamOptions: JetStreamOptions = JetStreamOptions.DEFAULT_JS_OPTIONS

/**
 * DSL 블록으로 [JetStreamOptions]를 생성합니다.
 *
 * @param builder [JetStreamOptions.Builder]에 적용할 설정 블록
 * @return [JetStreamOptions] 인스턴스
 */
inline fun jetStreamOptions(
    builder: JetStreamOptions.Builder.() -> Unit,
): JetStreamOptions = JetStreamOptions.builder().apply(builder).build()

/**
 * 주요 파라미터를 직접 지정하여 [JetStreamOptions]를 생성합니다.
 *
 * @param prefix JetStream API prefix (null이면 기본값 사용)
 * @param requestTimeout 요청 타임아웃 (null이면 기본값 사용)
 * @param publishNoAck ACK 없는 발행 여부 (null이면 기본값 사용)
 * @param optOut290ConsumerCreate 2.9.0 consumer create opt-out 여부
 * @param builder 추가 설정 블록
 * @return [JetStreamOptions] 인스턴스
 */
inline fun jetStreamOptionsOf(
    prefix: String? = null,
    requestTimeout: Duration? = null,
    publishNoAck: Boolean? = null,
    optOut290ConsumerCreate: Boolean? = null,
    builder: JetStreamOptions.Builder.() -> Unit = {},
): JetStreamOptions =
    jetStreamOptions {
        prefix?.run { prefix(this) }
        requestTimeout?.run { requestTimeout(this) }
        publishNoAck?.run { publishNoAck(this) }
        optOut290ConsumerCreate?.run { optOut290ConsumerCreate(this) }
        builder()
    }
