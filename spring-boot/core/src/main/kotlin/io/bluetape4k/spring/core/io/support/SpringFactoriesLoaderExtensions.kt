package io.bluetape4k.spring.core.io.support

import org.springframework.core.io.support.SpringFactoriesLoader

/**
 * receiver에 설정된 resource와 classloader를 사용해 reified factory 타입을 로드합니다.
 *
 * 호출자는 해당 factory resource와 classloader, 그리고 로드되는 구현을 신뢰해야 합니다. Factory 클래스의 초기화와
 * 생성자, [argumentResolver] callback은 임의 코드를 실행하거나 부수 효과를 일으킬 수 있습니다. 이 확장은 factory를
 * 별도로 검증하거나 격리하지 않고 sandbox를 제공하지 않습니다.
 *
 * [argumentResolver]와 그 callback의 결과는 변환 없이 Spring에 전달되므로 결과가 `null`인 경우도 Spring의 동작을
 * 따릅니다. [failureHandler]를 지정하지 않으면 Spring 기본 handler가 factory 로딩 실패를 예외로 전파합니다.
 * 사용자 handler가 실패 후 정상 반환하면 해당 factory는 생략되고 성공한 factory만 포함한 일부 목록이 반환될 수
 * 있습니다. 모든 provider가 필요하다면 handler가 예외를 던지도록 하세요. 일부 provider를 선택 사항으로 둘 때만
 * 실패를 기록하고 부분 결과를 명시적으로 허용하세요.
 */
inline fun <reified T : Any> SpringFactoriesLoader.load(
    argumentResolver: SpringFactoriesLoader.ArgumentResolver? = null,
    failureHandler: SpringFactoriesLoader.FailureHandler? = null,
): List<T> = load(T::class.java, argumentResolver, failureHandler)
