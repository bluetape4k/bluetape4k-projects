package io.bluetape4k.crypto.encrypt

/**
 * 기본 비밀번호
 *
 * > **⚠️ 보안 경고**: 이 값은 개발/테스트 환경 전용입니다.
 * > 프로덕션 환경에서는 반드시 `password` 파라미터에 충분히 길고 예측 불가능한
 * > 비밀번호를 명시적으로 전달하세요. 기본값을 그대로 사용하면 암호화 안전성이
 * > 보장되지 않습니다.
 *
 * 비밀번호의 길이가 짧으면 특정 Encryptor 는 오류를 발생시킨다. 최소 12자 이상의 비밀번호를 사용하자.
 */
internal const val DEFAULT_PASSWORD = "bluetape4k-default-crypto-key-for-dev-only"
