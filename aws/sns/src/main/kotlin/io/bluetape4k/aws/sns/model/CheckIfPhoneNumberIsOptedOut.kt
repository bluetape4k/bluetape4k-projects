package io.bluetape4k.aws.sns.model

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.sns.model.CheckIfPhoneNumberIsOptedOutRequest

/**
 * DSL 블록으로 [CheckIfPhoneNumberIsOptedOutRequest]를 빌드합니다.
 *
 * ## 동작/계약
 * - [builder] 블록에서 `phoneNumber` 등을 직접 설정한다.
 *
 * ```kotlin
 * val req = checkIfPhoneNumberIsOptedOutRequest {
 *     phoneNumber("+821012345678")
 * }
 * ```
 */
inline fun checkIfPhoneNumberIsOptedOutRequest(
    @BuilderInference builder: CheckIfPhoneNumberIsOptedOutRequest.Builder.() -> Unit,
): CheckIfPhoneNumberIsOptedOutRequest =
    CheckIfPhoneNumberIsOptedOutRequest.builder().apply(builder).build()

/**
 * 전화번호로 SMS 수신 거부 여부 조회 [CheckIfPhoneNumberIsOptedOutRequest]를 생성합니다.
 *
 * ## 동작/계약
 * - [phoneNumber]가 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val req = checkIfPhoneNumberIsOptedOutRequestOf("+821012345678")
 * // req.phoneNumber() == "+821012345678"
 * ```
 */
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
