package io.bluetape4k.hibernate.querydsl.simple

import com.querydsl.core.annotations.QueryProjection
import java.io.Serializable

/**
 * [QueryProjection] 을 활용하여 Entity를 거지치 않고, 바로 DTO로 변환할 수 있다.
 */
data class ExampleDto @QueryProjection constructor(
    val id: Long,
    val name: String,
): Serializable
