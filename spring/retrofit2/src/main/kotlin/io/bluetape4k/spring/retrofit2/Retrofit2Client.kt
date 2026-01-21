package io.bluetape4k.spring.retrofit2

import org.springframework.core.annotation.AliasFor
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@MustBeDocumented
@Service
annotation class Retrofit2Client(

    /**
     * Retrofit2 Client의 Identifier에 해당 ([name] 과 같다)
     * 스프링 환경설정이나 시스템 설정에서 값을 가져올 때에는 `value=\${property.key}` 처럼 지정할 수 있다.
     */
    @get:AliasFor("name")
    val value: String = "",

    /**
     * [value]와 같다.
     */
    @get:AliasFor("value")
    val name: String = "",

    /**
     * 스프링의 [org.springframework.beans.factory.annotation.Qualifier] annotation을 쓸 경우에 지정
     */
    val qualifier: String = "",

    /**
     * 호출할 REST API의 Base URL
     */
    val baseUrl: String = "",

    /**
     * Retrofit2 Client를 위한 사용자 정의 [org.springframework.context.annotation.Configuration] 입니다.
     * 시스템의 기본 설정 이외에 추가 설정할 수 있게 해줍니다.
     */
    val configuration: Array<KClass<*>> = [],
)
