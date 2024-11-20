package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.services.sns.model.CheckIfPhoneNumberIsOptedOutRequest

inline fun CheckIfPhoneNumberIsOptedOutRequest(
    initializer: CheckIfPhoneNumberIsOptedOutRequest.Builder.() -> Unit,
): CheckIfPhoneNumberIsOptedOutRequest =
    CheckIfPhoneNumberIsOptedOutRequest.builder().apply(initializer).build()

fun checkIfPhoneNumberIsOptedOutRequestOf(
    phoneNumber: String,
    initializer: CheckIfPhoneNumberIsOptedOutRequest.Builder.() -> Unit = {},
): CheckIfPhoneNumberIsOptedOutRequest = CheckIfPhoneNumberIsOptedOutRequest {
    phoneNumber(phoneNumber)
    initializer()
}
