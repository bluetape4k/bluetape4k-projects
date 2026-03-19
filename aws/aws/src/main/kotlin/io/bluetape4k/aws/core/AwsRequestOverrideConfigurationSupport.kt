package io.bluetape4k.aws.core

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration

/**
 * [AwsRequestOverrideConfiguration]을 빌더 DSL로 생성합니다.
 *
 * ## 동작/계약
 * - [AwsRequestOverrideConfiguration.builder]로 새 빌더를 만든 뒤 [builder]를 적용한다.
 * - [builder] 실행 후 `build()` 결과를 즉시 반환한다.
 *
 * ```kotlin
 * val configuration = awsRequestOverrideConfiguration {
 *     apiCallTimeout(java.time.Duration.ofSeconds(1))
 * }
 * // configuration.apiCallTimeout().isPresent == true
 * ```
 */
inline fun awsRequestOverrideConfiguration(
    builder: AwsRequestOverrideConfiguration.Builder.() -> Unit,
): AwsRequestOverrideConfiguration {
    return AwsRequestOverrideConfiguration.builder().apply(builder).build()
}

/**
 * 요청 단위 자격 증명 공급자를 지정한 [AwsRequestOverrideConfiguration]을 생성합니다.
 *
 * ## 동작/계약
 * - 내부 [awsRequestOverrideConfiguration]에서 `credentialsProvider(credentialsProvider)`를 호출한다.
 * - 반환 객체는 전달된 provider를 요청 오버라이드 자격 증명으로 포함한다.
 *
 * ```kotlin
 * val provider = software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider.create()
 * val configuration = awsRequestOverrideConfigurationOf(provider)
 * // configuration.credentialsProvider().orElse(null) == provider
 * ```
 */
fun awsRequestOverrideConfigurationOf(
    credentialsProvider: AwsCredentialsProvider,
): AwsRequestOverrideConfiguration {
    return awsRequestOverrideConfiguration {
        credentialsProvider(credentialsProvider)
    }
}
