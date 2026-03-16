package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.ListTopicsRequest

/**
 * DSL 블록으로 [ListTopicsRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `nextToken` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = listTopicsRequest { nextToken("token123") }
 * ```
 */
inline fun listTopicsRequest(
    builder: ListTopicsRequest.Builder.() -> Unit,
): ListTopicsRequest =
    ListTopicsRequest.builder().apply(builder).build()

/**
 * 페이지네이션 토큰으로 [ListTopicsRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [nextToken]이 non-null이면서 blank이면 `IllegalArgumentException`을 던진다.
 * - [nextToken]이 null이면 첫 페이지를 조회한다.
 *
 * ```kotlin
 * val req = listTopicsRequestOf()
 * // 첫 페이지 토픽 목록 요청
 * ```
 */
inline fun listTopicsRequestOf(
    nextToken: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: ListTopicsRequest.Builder.() -> Unit = {},
): ListTopicsRequest =
    listTopicsRequest {
        nextToken?.let {
            nextToken.requireNotBlank("nextToken")
            nextToken(it)
        }
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
