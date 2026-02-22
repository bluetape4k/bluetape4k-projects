package io.bluetape4k.exposed.core

/**
 * 식별자를 가지는 엔티티를 나타내는 인터페이스입니다.
 *
 * @param ID 엔티티의 식별자 타입
 * @property id 엔티티의 고유 식별자
 */
interface HasIdentifier<ID: Any>: java.io.Serializable {
    val id: ID
}
