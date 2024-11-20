package io.bluetape4k.spring.retrofit2

import org.springframework.context.annotation.Import
import org.springframework.core.annotation.AliasFor
import kotlin.reflect.KClass

/**
 * [Retrofit2Client] annotation이 적용된 class를 scanning 해서 Retrofit Client로 빌드하고자 할 때 사용합니다.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@MustBeDocumented
@Import(RetrofitClientsRegistrar::class)
annotation class EnableRetrofitClients(
    /**
     * Alias for [basePackages]
     */
    @get:AliasFor("basePackages")
    val value: Array<String> = [],

    /**
     * [Retrofit2Client] annotation이 적용된 class를 scan하기 위한 기본 class path를 나타냅니다.
     */
    @get:AliasFor("value")
    val basePackages: Array<String> = [],

    /**
     * [Retrofit2Client] annotation이 적용된 class를 scan하기 위한 기본 class path를 나타냅니다.
     */
    val basePackageClasses: Array<KClass<*>> = [],

    /**
     * 모든 Retrofit Clients에게 적용될 기본 `@Configuration` 를 지정하며,
     * 이 Configuration은 Retrofit Client를 구성하기 위한 Bean이 정의되어야 한다.
     */
    val defaultConfiguration: Array<KClass<*>> = [],

    /**
     * [Retrofit2Client] annotation이 적용된 class의 배열, 이 값이 지정되면 classpath scanning은 하지 않습니다.
     */
    val clients: Array<KClass<*>> = [],
)
