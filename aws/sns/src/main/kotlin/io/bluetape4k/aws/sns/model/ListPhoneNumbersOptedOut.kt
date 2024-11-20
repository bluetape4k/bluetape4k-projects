package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.ListPhoneNumbersOptedOutRequest

inline fun ListPhoneNumbersOptedOutRequest(
    initializer: ListPhoneNumbersOptedOutRequest.Builder.() -> Unit,
): ListPhoneNumbersOptedOutRequest =
    ListPhoneNumbersOptedOutRequest.builder().apply(initializer).build()

fun listPhoneNumbersOptedOutRequestOf(
    nextToken: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    initializer: ListPhoneNumbersOptedOutRequest.Builder.() -> Unit = {},
): ListPhoneNumbersOptedOutRequest = ListPhoneNumbersOptedOutRequest {
    nextToken?.run { nextToken(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    initializer()
}
