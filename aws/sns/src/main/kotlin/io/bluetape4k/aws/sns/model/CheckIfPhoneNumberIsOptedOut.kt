package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.model.CheckIfPhoneNumberIsOptedOutRequest

inline fun checkIfPhoneNumberIsOptedOutRequest(
    @BuilderInference builder: CheckIfPhoneNumberIsOptedOutRequest.Builder.() -> Unit,
): CheckIfPhoneNumberIsOptedOutRequest =
    CheckIfPhoneNumberIsOptedOutRequest.builder().apply(builder).build()

inline fun checkIfPhoneNumberIsOptedOutRequestOf(
    phoneNumber: String,
    @BuilderInference bulider: CheckIfPhoneNumberIsOptedOutRequest.Builder.() -> Unit = {},
): CheckIfPhoneNumberIsOptedOutRequest {
    phoneNumber.requireNotBlank("phoneNumber")

    return checkIfPhoneNumberIsOptedOutRequest {
        phoneNumber(phoneNumber)
        bulider()
    }
}
