package io.bluetape4k.tenant

/**
 * application boundary에서 검증·정규화된 tenant 식별자입니다.
 *
 * blank 값만 거부하며 입력 문자열을 trim하거나 대소문자 변환하지 않습니다.
 */
@JvmInline
value class TenantId(val value: String) {
    init {
        require(value.isNotBlank()) { "TenantId must not be blank" }
    }
}
