package io.bluetape4k.tink.keyset

import com.google.crypto.tink.KeysetHandle
import java.time.Instant

/**
 * 버전과 생성 시각을 함께 보관하는 Tink [KeysetHandle] 래퍼입니다.
 *
 * 새 암호화는 현재 active version으로 수행하고, 복호화는 암호문에 저장된 version으로
 * 적절한 keyset을 찾아 수행하는 용도로 사용합니다.
 *
 * @property version 키셋 버전. `1`부터 증가합니다.
 * @property createdAt 키셋 생성 시각입니다.
 * @property keysetHandle 실제 Tink 키셋 핸들입니다.
 */
data class VersionedKeysetHandle(
    val version: Long,
    val createdAt: Instant,
    val keysetHandle: KeysetHandle,
)
