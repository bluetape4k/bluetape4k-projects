package io.bluetape4k.exposed.trino

/**
 * Trino에서 지원하지 않는 기능임을 나타내는 마커 어노테이션입니다.
 *
 * 컴파일 타임 경고 또는 테스트 건너뛰기 용도로 사용합니다.
 * [reason]에 미지원 이유나 대안을 기재하세요.
 *
 * @param reason 미지원 이유 또는 대안 설명
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TrinoUnsupported(val reason: String = "")
