package io.bluetape4k.spring.data.exposed.jdbc.annotation

/**
 * Repository 메서드에 raw SQL 쿼리를 지정합니다.
 *
 * @param value 실행할 SQL 쿼리 문자열 (위치 기반 파라미터 ?1, ?2, ... 사용)
 * @param countQuery 페이징 시 count 쿼리 (생략 시 value 쿼리 기반으로 자동 생성)
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Query(val value: String, val countQuery: String = "")
