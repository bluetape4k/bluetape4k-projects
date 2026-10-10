package io.bluetape4k.spring.tests

import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import java.util.function.Supplier

/**
 * 신뢰하는 설정 클래스를 이 [ApplicationContextRunner]에 등록합니다.
 *
 * 설정 클래스를 검증하거나 격리하지 않습니다. 설정 분석, 조건 평가, bean과 factory method 실행 및 예외 전파는
 * Spring context 수명주기를 따르며, 이 함수는 context를 refresh하지 않습니다.
 */
inline fun <reified CONFIG : Any> ApplicationContextRunner.withUserConfiguration(): ApplicationContextRunner =
    withUserConfiguration(CONFIG::class.java)

/**
 * 신뢰하는 설정 클래스를 이 [WebApplicationContextRunner]에 등록합니다.
 *
 * 설정 클래스를 검증하거나 격리하지 않습니다. 설정 분석, 조건 평가, bean과 factory method 실행 및 예외 전파는
 * Spring context 수명주기를 따르며, 이 함수는 context를 refresh하지 않습니다.
 */
inline fun <reified CONFIG : Any> WebApplicationContextRunner.withUserConfiguration():
    WebApplicationContextRunner =
    withUserConfiguration(CONFIG::class.java)

/**
 * 신뢰하는 설정 클래스를 이 [ReactiveWebApplicationContextRunner]에 등록합니다.
 *
 * 설정 클래스를 검증하거나 격리하지 않습니다. 설정 분석, 조건 평가, bean과 factory method 실행 및 예외 전파는
 * Spring context 수명주기를 따르며, 이 함수는 context를 refresh하지 않습니다.
 */
inline fun <reified CONFIG : Any> ReactiveWebApplicationContextRunner.withUserConfiguration():
    ReactiveWebApplicationContextRunner =
    withUserConfiguration(CONFIG::class.java)

/**
 * Spring이 bean을 생성할 때 호출할 공급자를 등록합니다.
 *
 * Spring이 이름을 생성하며 [customizers]를 bean definition에 전달합니다. [supplier]는 함수 호출 중 실행하지
 * 않으며, eager bean은 context refresh 중, lazy bean은 최초 조회 시 Spring이 호출합니다.
 */
inline fun <reified T : Any> ApplicationContextRunner.withBean(
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): ApplicationContextRunner =
    withBean(T::class.java, Supplier { supplier() }, *customizers)

/**
 * 지정한 이름 또는 Spring이 생성한 이름으로 bean 공급자를 등록합니다.
 *
 * [customizers]는 bean definition에 전달합니다. [supplier]는 함수 호출 중 실행하지 않으며, eager bean은 context
 * refresh 중, lazy bean은 최초 조회 시 Spring이 호출합니다.
 */
inline fun <reified T : Any> ApplicationContextRunner.withBean(
    name: String?,
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): ApplicationContextRunner =
    withBean(name, T::class.java, Supplier { supplier() }, *customizers)

/**
 * Spring이 bean을 생성할 때 호출할 공급자를 등록합니다.
 *
 * Spring이 이름을 생성하며 [customizers]를 bean definition에 전달합니다. [supplier]는 함수 호출 중 실행하지
 * 않으며, eager bean은 context refresh 중, lazy bean은 최초 조회 시 Spring이 호출합니다.
 */
inline fun <reified T : Any> WebApplicationContextRunner.withBean(
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): WebApplicationContextRunner =
    withBean(T::class.java, Supplier { supplier() }, *customizers)

/**
 * 지정한 이름 또는 Spring이 생성한 이름으로 bean 공급자를 등록합니다.
 *
 * [customizers]는 bean definition에 전달합니다. [supplier]는 함수 호출 중 실행하지 않으며, eager bean은 context
 * refresh 중, lazy bean은 최초 조회 시 Spring이 호출합니다.
 */
inline fun <reified T : Any> WebApplicationContextRunner.withBean(
    name: String?,
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): WebApplicationContextRunner =
    withBean(name, T::class.java, Supplier { supplier() }, *customizers)

/**
 * Spring이 bean을 생성할 때 호출할 공급자를 등록합니다.
 *
 * Spring이 이름을 생성하며 [customizers]를 bean definition에 전달합니다. [supplier]는 함수 호출 중 실행하지
 * 않으며, eager bean은 context refresh 중, lazy bean은 최초 조회 시 Spring이 호출합니다.
 */
inline fun <reified T : Any> ReactiveWebApplicationContextRunner.withBean(
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): ReactiveWebApplicationContextRunner =
    withBean(T::class.java, Supplier { supplier() }, *customizers)

/**
 * 지정한 이름 또는 Spring이 생성한 이름으로 bean 공급자를 등록합니다.
 *
 * [customizers]는 bean definition에 전달합니다. [supplier]는 함수 호출 중 실행하지 않으며, eager bean은 context
 * refresh 중, lazy bean은 최초 조회 시 Spring이 호출합니다.
 */
inline fun <reified T : Any> ReactiveWebApplicationContextRunner.withBean(
    name: String?,
    vararg customizers: BeanDefinitionCustomizer,
    crossinline supplier: () -> T,
): ReactiveWebApplicationContextRunner =
    withBean(name, T::class.java, Supplier { supplier() }, *customizers)
