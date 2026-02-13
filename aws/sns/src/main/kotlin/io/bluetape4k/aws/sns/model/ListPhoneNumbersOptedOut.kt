package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration
import software.amazon.awssdk.services.sns.model.ListPhoneNumbersOptedOutRequest

inline fun listPhoneNumbersOptedOutRequest(
    @BuilderInference builder: ListPhoneNumbersOptedOutRequest.Builder.() -> Unit,
): ListPhoneNumbersOptedOutRequest =
    ListPhoneNumbersOptedOutRequest.builder().apply(builder).build()

inline fun listPhoneNumbersOptedOutRequestOf(
    nextToken: String? = null,
    overrideConfiguration: AwsRequestOverrideConfiguration? = null,
    @BuilderInference builder: ListPhoneNumbersOptedOutRequest.Builder.() -> Unit = {},
): ListPhoneNumbersOptedOutRequest =
    listPhoneNumbersOptedOutRequest {
        nextToken?.let {
            nextToken.requireNotBlank("nextToken")
            nextToken(it)
        }
        overrideConfiguration?.let { overrideConfiguration(it) }

        builder()
    }
