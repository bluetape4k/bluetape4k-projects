package io.bluetape4k.exposed.bigquery

/**
 * BigQuery REST API 쿼리 실행 중 발생한 오류를 나타내는 예외.
 *
 * [BigQueryContext.runRawQuery] 또는 페이지네이션 과정에서 BigQuery 서버가 오류를 반환할 때 던져집니다.
 *
 * ```kotlin
 * try {
 *     context.runRawQuery("INVALID SQL")
 * } catch (e: BigQueryQueryException) {
 *     log.error { "BigQuery 쿼리 오류: ${e.message}" }
 * }
 * ```
 *
 * @param message BigQuery 서버에서 반환된 오류 메시지
 * @param cause 원인 예외 (있는 경우)
 */
class BigQueryQueryException(
    message: String,
    cause: Throwable? = null,
): RuntimeException(message, cause)
