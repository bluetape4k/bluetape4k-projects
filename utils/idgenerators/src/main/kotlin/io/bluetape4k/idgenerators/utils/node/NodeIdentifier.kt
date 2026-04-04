package io.bluetape4k.idgenerators.utils.node

/**
 * 사용자 정의 노드 식별자를 Flake ID 생성기에 제공하기 위해 구현해야하는 인터페이스
 *
 * ```kotlin
 * val nodeId: NodeIdentifier = NodeIdentifier { 42L }
 * val id: Long = nodeId.get()
 * // id == 42L
 * ```
 */
fun interface NodeIdentifier {

    /**
     * 노드 식별자 값을 Long으로 반환합니다.
     *
     * ```kotlin
     * val nodeId: NodeIdentifier = NodeIdentifier { 1234L }
     * val id: Long = nodeId.get()
     * // id == 1234L
     * ```
     *
     * @return 노드 식별자 Long 값
     */
    fun get(): Long
}
