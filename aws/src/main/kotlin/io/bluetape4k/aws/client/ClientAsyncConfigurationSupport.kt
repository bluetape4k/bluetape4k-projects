package io.bluetape4k.aws.client

import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption

/**
 * [ClientAsyncConfiguration]을 빌더 DSL로 생성합니다.
 *
 * ## 동작/계약
 * - [ClientAsyncConfiguration.builder]로 생성한 빌더에 [builder]를 적용한다.
 * - [builder] 적용 후 `build()` 결과를 반환한다.
 *
 * ```kotlin
 * val config = clientAsyncConfiguration {
 *     advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, java.util.concurrent.Executors.newSingleThreadExecutor())
 * }
 * // config.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR) != null
 * ```
 */
inline fun clientAsyncConfiguration(
    builder: ClientAsyncConfiguration.Builder.() -> Unit,
): ClientAsyncConfiguration {
    return ClientAsyncConfiguration.builder().apply(builder).build()
}

/**
 * 단일 비동기 고급 옵션으로 [ClientAsyncConfiguration]을 생성합니다.
 *
 * ## 동작/계약
 * - 내부 [clientAsyncConfiguration] 블록에서 `advancedOption(asyncOption, value)`를 1회 호출한다.
 * - 전달한 옵션과 값이 생성 결과에 그대로 반영된다.
 *
 * ```kotlin
 * val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
 * val config = clientAsyncConfigurationOf(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, executor)
 * // config.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR) == executor
 * ```
 */
fun <T> clientAsyncConfigurationOf(
    asyncOption: SdkAdvancedAsyncClientOption<T>,
    value: T,
): ClientAsyncConfiguration = clientAsyncConfiguration {
    advancedOption(asyncOption, value)
}
