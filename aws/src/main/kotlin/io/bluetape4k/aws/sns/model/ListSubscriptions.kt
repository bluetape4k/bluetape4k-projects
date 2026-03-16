package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.ListSubscriptionsRequest

/**
 * DSL 블록으로 [ListSubscriptionsRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `nextToken` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = listSubscriptionsRequest { nextToken("token123") }
 * ```
 */
inline fun listSubscriptionsRequest(
    @BuilderInference builder: ListSubscriptionsRequest.Builder.() -> Unit,
): ListSubscriptionsRequest =
    ListSubscriptionsRequest.builder().apply(builder).build()

/**
 * 페이지네이션 토큰으로 [ListSubscriptionsRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [nextToken]이 non-null이면서 blank이면 `IllegalArgumentException`을 던진다.
 * - [nextToken]이 null이면 첫 페이지를 조회한다.
 *
 * ```kotlin
 * val req = listSubscriptionsRequestOf()
 * // 첫 페이지 구독 목록 요청
 * ```
 */
inline fun listSubscriptionsRequestOf(
    nextToken: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: ListSubscriptionsRequest.Builder.() -> Unit = {},
): ListSubscriptionsRequest =
    listSubscriptionsRequest {
        nextToken?.let {
            nextToken.requireNotBlank("nextToken")
            nextToken(it)
        }
        overrideConfiguration?.let { overrideConfiguration(it) }
        builder()
    }
