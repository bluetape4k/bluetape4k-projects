package io.bluetape4k.nats.client.api

import io.nats.client.FetchConsumeOptions

/**
 * [FetchConsumeOptions]를 DSL로 생성합니다.
 */
inline fun fetchConsumeOptions(
    builder: FetchConsumeOptions.Builder.() -> Unit,
): FetchConsumeOptions = FetchConsumeOptions.builder().apply(builder).build()

/** 기본 최대 메시지 수 */
private const val DEFAULT_MAX_MESSAGES = 100

/** 기본 만료 시간 (밀리초) */
private const val DEFAULT_EXPIRES_IN_MILLIS = 1000L

/**
 * 메시지 개수/만료 시간/바이트 한도로 [FetchConsumeOptions]를 생성합니다.
 *
 * @param maxMessages 한 번에 가져올 최대 메시지 수 (기본값: [DEFAULT_MAX_MESSAGES])
 * @param expiresInMillis 요청 만료 시간(밀리초) (기본값: [DEFAULT_EXPIRES_IN_MILLIS])
 * @param maxBytes 최대 바이트 한도 (null이면 제한 없음)
 * @return [FetchConsumeOptions] 인스턴스
 */
fun fetchConsumeOptionsOf(
    maxMessages: Int = DEFAULT_MAX_MESSAGES,
    expiresInMillis: Long = DEFAULT_EXPIRES_IN_MILLIS,
    maxBytes: Long? = null,
): FetchConsumeOptions =
    fetchConsumeOptions {
        maxMessages(maxMessages)
        expiresIn(expiresInMillis)
        maxBytes?.run { maxBytes(this) }
    }
