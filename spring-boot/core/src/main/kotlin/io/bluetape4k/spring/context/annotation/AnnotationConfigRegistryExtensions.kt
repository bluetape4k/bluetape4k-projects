package io.bluetape4k.spring.context.annotation

import org.springframework.context.annotation.AnnotationConfigRegistry

/**
 * reified 설정 클래스를 [AnnotationConfigRegistry.register]에 등록합니다.
 *
 * 애플리케이션 또는 테스트에서 신뢰하는 설정 코드를 전달해야 합니다. 이 함수는 클래스를 검증하거나 격리하지
 * 않습니다. 설정 분석, 조건 평가, bean 및 factory method 실행과 예외 전파는 Spring context 생명주기를 따릅니다.
 * 등록된 클래스 외의 다른 클래스를 추가하거나 context를 refresh하지 않습니다.
 */
inline fun <reified T : Any> AnnotationConfigRegistry.register() {
    register(T::class.java)
}
