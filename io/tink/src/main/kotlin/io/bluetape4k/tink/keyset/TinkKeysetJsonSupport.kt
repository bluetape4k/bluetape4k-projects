package io.bluetape4k.tink.keyset

import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkJsonProtoKeysetFormat
import io.bluetape4k.support.requireNotBlank

/**
 * [KeysetHandle]을 JSON 문자열로 직렬화합니다.
 *
 * 운영 환경에서는 이 JSON을 그대로 외부에 노출하지 말고, KMS/HSM 또는 별도 보호 계층과 함께 사용하세요.
 */
fun KeysetHandle.toJsonKeyset(): String =
    TinkJsonProtoKeysetFormat.serializeKeyset(this, InsecureSecretKeyAccess.get())

/**
 * JSON 문자열에서 [KeysetHandle]을 복원합니다.
 *
 * @param jsonKeyset [toJsonKeyset]으로 직렬화한 JSON 문자열
 */
fun keysetHandleOf(jsonKeyset: String): KeysetHandle =
    TinkJsonProtoKeysetFormat.parseKeyset(jsonKeyset.requireNotBlank("jsonKeyset"), InsecureSecretKeyAccess.get())
