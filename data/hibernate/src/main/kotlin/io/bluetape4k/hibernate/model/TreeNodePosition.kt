package io.bluetape4k.hibernate.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable

/**
 * Tree 구조 상에서 Node의 위치를 나타내는 Value Object입니다.
 */
@Embeddable
data class TreeNodePosition(
    @Column(name = "nodeLevel") val nodeLevel: Int = 0,
    @Column(name = "nodeOrder") val nodeOrder: Int = 0,
): Serializable
