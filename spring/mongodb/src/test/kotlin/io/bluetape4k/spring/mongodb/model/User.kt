package io.bluetape4k.spring.mongodb.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * Spring Data MongoDB 통합 테스트에 사용하는 사용자 도메인 모델입니다.
 *
 * `@Document` 어노테이션으로 MongoDB 컬렉션 이름을 `test_users`로 지정합니다.
 * 테스트 간 충돌 방지를 위해 전용 컬렉션 이름을 사용합니다.
 */
@Document(collection = "test_users")
data class User(
    /** MongoDB 문서 ID. 삽입 전에는 null입니다. */
    @Id val id: String? = null,

    /** 사용자 이름 */
    val name: String,

    /** 이메일 주소 (인덱싱됨) */
    @Indexed(unique = true)
    val email: String,

    /** 나이 */
    val age: Int,

    /** 거주 도시 */
    val city: String,
)
