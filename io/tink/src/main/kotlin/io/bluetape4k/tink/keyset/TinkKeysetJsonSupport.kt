package io.bluetape4k.tink.keyset

import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.TinkJsonProtoKeysetFormat
import io.bluetape4k.support.requireNotBlank

/**
 * [KeysetHandle]을 JSON 문자열로 직렬화합니다.
 *
 * 반환 문자열에는 secret key material이 평문으로 포함됩니다. 운영 환경에서는 이 JSON을 그대로 외부에 노출하지 말고,
 * KMS/HSM 기반 envelope encryption, 저장소 접근 제어, 백업 암호화 같은 별도 보호 계층과 함께 사용하세요.
 *
 * ```kotlin
 * val json = keysetHandle.toJsonKeyset()
 * // json은 암호화 키 자체를 포함하므로 안전한 secret 저장소에만 기록하세요.
 * ```
 */
fun KeysetHandle.toJsonKeyset(): String =
    TinkJsonProtoKeysetFormat.serializeKeyset(this, InsecureSecretKeyAccess.get())

/**
 * JSON 문자열에서 [KeysetHandle]을 복원합니다.
 *
 * 입력 JSON은 [toJsonKeyset]으로 만든 cleartext keyset입니다. 신뢰할 수 없는 입력을 파싱하지 말고,
 * 저장소에서 읽은 값의 접근 경로와 무결성을 별도로 보호하세요.
 *
 * @param jsonKeyset [toJsonKeyset]으로 직렬화한 JSON 문자열
 */
fun keysetHandleOf(jsonKeyset: String): KeysetHandle =
    TinkJsonProtoKeysetFormat.parseKeyset(jsonKeyset.requireNotBlank("jsonKeyset"), InsecureSecretKeyAccess.get())
