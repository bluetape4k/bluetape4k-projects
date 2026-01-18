package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.ListPhoneNumbersOptedOutRequest

inline fun ListPhoneNumbersOptedOutRequest(
    builder: ListPhoneNumbersOptedOutRequest.Builder.() -> Unit,
): ListPhoneNumbersOptedOutRequest =
    ListPhoneNumbersOptedOutRequest.builder().apply(builder).build()

inline fun listPhoneNumbersOptedOutRequestOf(
    nextToken: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    builder: ListPhoneNumbersOptedOutRequest.Builder.() -> Unit = {},
): ListPhoneNumbersOptedOutRequest = ListPhoneNumbersOptedOutRequest {
    nextToken?.run { nextToken(this) }
    overrideConfiguration?.run { overrideConfiguration(this) }

    builder()
}
