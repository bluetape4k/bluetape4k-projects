package io.bluetape4k.spring.retrofit2

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 * Retrofit 클라이언트별 자식 애플리케이션 컨텍스트를 관리한다.
 *
 * ## 동작/계약
 * - Spring Cloud `NamedContextFactory` 없이 Spring Boot 4와 호환되는 독립 구현체다.
 * - 각 클라이언트 이름마다 별도의 자식 [AnnotationConfigApplicationContext]를 생성한다.
 * - `setConfigurations()`로 등록된 [RetrofitClientSpecification] 명세를 기반으로 컨텍스트를 구성한다.
 * - 기본 설정 타입으로 [DefaultRetrofitClientConfiguration]을 항상 포함한다.
 *
 * ```kotlin
 * val context = RetrofitClientContext()
 * context.setConfigurations(listOf(RetrofitClientSpecification("httpbin", emptyArray())))
 * val hasConfig = context.configurations.containsKey("httpbin")
 * // hasConfig == true
 * ```
 */
class RetrofitClientContext: ApplicationContextAware {

    companion object: KLogging()

    private lateinit var parent: ApplicationContext
    private val specs = mutableMapOf<String, RetrofitClientSpecification>()
    private val contexts = mutableMapOf<String, AnnotationConfigApplicationContext>()

    /** 등록된 [RetrofitClientSpecification] 목록이다. */
    val configurations: Map<String, RetrofitClientSpecification>
        get() = specs.toMap()

    /**
     * [RetrofitClientSpecification] 목록을 등록한다.
     *
     * @param specifications 클라이언트별 설정 명세 목록
     */
    fun setConfigurations(specifications: List<RetrofitClientSpecification>) {
        specifications.forEach { specs[it.getName()] = it }
    }

    /**
     * 지정된 이름의 컨텍스트에서 타입에 해당하는 단일 빈을 반환한다.
     *
     * @param name 클라이언트 이름
     * @param type 조회할 빈 타입
     * @return 빈이 없으면 null
     */
    fun <T: Any> getInstance(name: String, type: Class<T>): T? {
        return try {
            getContext(name).getBean(type)
        } catch (e: Exception) {
            log.debug { "No bean of type ${type.name} for client '$name'" }
            null
        }
    }

    /**
     * 지정된 이름의 컨텍스트에서 타입에 해당하는 모든 빈을 반환한다.
     *
     * @param name 클라이언트 이름
     * @param type 조회할 빈 타입
     * @return 빈이 없으면 null
     */
    fun <T: Any> getInstances(name: String, type: Class<T>): Map<String, T>? {
        return try {
            getContext(name).getBeansOfType(type).takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            log.debug { "No beans of type ${type.name} for client '$name'" }
            null
        }
    }

    private fun getContext(name: String): ApplicationContext {
        return contexts.getOrPut(name) {
            log.debug { "Creating child context for Retrofit client '$name'" }
            AnnotationConfigApplicationContext().apply {
                parent = this@RetrofitClientContext.parent
                specs[name]?.getConfiguration()?.forEach { register(it) }
                register(DefaultRetrofitClientConfiguration::class.java)
                refresh()
            }
        }
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.parent = applicationContext
    }
}
