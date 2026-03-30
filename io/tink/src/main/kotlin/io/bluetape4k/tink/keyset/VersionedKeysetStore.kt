package io.bluetape4k.tink.keyset

import java.time.Duration

/**
 * 버전 관리되는 Tink keyset 저장소 추상화입니다.
 *
 * 구현체는 active version 조회, 특정 version 로드, 새 keyset 회전(rotation)을 담당합니다.
 */
interface VersionedKeysetStore {

    /**
     * 현재 active keyset을 반환합니다.
     *
     * 저장소가 비어 있으면 구현체 정책에 따라 초기 keyset을 생성할 수 있습니다.
     */
    fun current(): VersionedKeysetHandle

    /**
     * 지정한 [version]의 keyset을 반환합니다.
     */
    fun find(version: Long): VersionedKeysetHandle?

    /**
     * 새 keyset을 생성하고 active version으로 승격합니다.
     */
    fun rotate(): VersionedKeysetHandle

    /**
     * [rotationPeriod]가 경과했을 때만 회전합니다.
     */
    fun rotateIfDue(rotationPeriod: Duration): VersionedKeysetHandle
}
