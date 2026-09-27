package io.bluetape4k.bucket4j

/**
 * 기본 Rate Limit Key 의 접두사
 */
const val DEFAULT_KEY_PREFIX = "bluetape4k.rate-limit.key."

/**
 * 요청당 허용하는 최대 토큰 수.
 *
 * 비정상적으로 큰 값에 의한 계산 오버플로우/비정상 요청을 방지하기 위한 상한선이다.
 */
const val MAX_TOKENS_PER_REQUEST: Long = 1_000_000_000_000L

/**
 * Maximum serialized bucket-key size accepted by bluetape4k Bucket4j providers.
 *
 * The cap is applied after the provider prefix is appended. It prevents
 * unbounded user-provided keys from amplifying local cache or Redis memory use.
 */
const val MAX_BUCKET_KEY_BYTES: Int = 512
