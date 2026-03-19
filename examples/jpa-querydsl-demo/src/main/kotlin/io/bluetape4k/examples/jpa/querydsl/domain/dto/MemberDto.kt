package io.bluetape4k.examples.jpa.querydsl.domain.dto

import com.querydsl.core.annotations.QueryProjection
import io.bluetape4k.examples.jpa.querydsl.domain.model.Member
import java.io.Serializable

data class MemberDto(
    val id: Long,
    val name: String,
    val age: Int? = 0,
): Serializable {

    /**
     * 이렇게도 가능하지만, 성능 상 좋은 점이 없다
     * 또한, DTO 모듈에 Entity 관련 기능이 포함되어 버린다
     */
    constructor(member: Member): this(member.id!!, member.name, member.age)
}

data class MemberVo @QueryProjection constructor(
    var id: Long? = null,
    var name: String? = null,
    var age: Int? = null,
): Serializable 
