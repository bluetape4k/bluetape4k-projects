package io.bluetape4k.idgenerators.utils.node

/**
 * 사용자 정의 노드 식별자를 Flake ID 생성기에 제공하기 위해 구현해야하는 인터페이스
 */
fun interface NodeIdentifier {

    fun get(): Long
}
