package io.bluetape4k.aws.sns.model

import software.amazon.awssdk.services.sns.model.CheckIfPhoneNumberIsOptedOutRequest

inline fun CheckIfPhoneNumberIsOptedOutRequest(
    @BuilderInference builder: CheckIfPhoneNumberIsOptedOutRequest.Builder.() -> Unit,
): CheckIfPhoneNumberIsOptedOutRequest =
    CheckIfPhoneNumberIsOptedOutRequest.builder().apply(builder).build()

inline fun checkIfPhoneNumberIsOptedOutRequestOf(
    phoneNumber: String,
    @BuilderInference bulider: CheckIfPhoneNumberIsOptedOutRequest.Builder.() -> Unit = {},
): CheckIfPhoneNumberIsOptedOutRequest = CheckIfPhoneNumberIsOptedOutRequest {
    phoneNumber(phoneNumber)
    bulider()
}
