package io.bluetape4k.spring.tests

import org.springframework.boot.autoconfigure.AutoConfigurations

/**
 * 신뢰하는 단일 auto-configuration 클래스에 대한 Spring Boot test 설정 묶음을 생성합니다.
 *
 * Spring [AutoConfigurations.of]에 직접 위임하며, 클래스를 검증하거나 격리하지 않습니다. 적용 순서, 조건 평가,
 * 설정 처리 및 실패 동작은 Spring Boot test context 수명주기를 따릅니다.
 */
inline fun <reified T : Any> autoConfigurationsOf(): AutoConfigurations =
    AutoConfigurations.of(T::class.java)
