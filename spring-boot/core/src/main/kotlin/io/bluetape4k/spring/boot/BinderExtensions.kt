package io.bluetape4k.spring.boot

import org.springframework.boot.context.properties.bind.BindHandler
import org.springframework.boot.context.properties.bind.BindResult
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder

/** 지정한 prefix를 reified 타입으로 바인딩합니다. */
inline fun <reified T : Any> Binder.bind(name: String): BindResult<T> =
    bind(name, T::class.java)

/** 지정한 prefix를 [BindHandler]와 함께 reified 타입으로 바인딩합니다. */
inline fun <reified T : Any> Binder.bind(name: String, bindHandler: BindHandler?): BindResult<T> =
    bind(name, Bindable.of(T::class.java), bindHandler)

/** 지정한 prefix를 reified 타입으로 바인딩하고, 바인딩되지 않은 값은 기본 인스턴스로 만듭니다. */
inline fun <reified T : Any> Binder.bindOrCreate(name: String): T =
    bindOrCreate(name, T::class.java)

/** 지정한 prefix를 [BindHandler]와 함께 바인딩하고, 결과 인스턴스를 반환합니다. */
inline fun <reified T : Any> Binder.bindOrCreate(name: String, bindHandler: BindHandler): T =
    bindOrCreate(name, Bindable.of(T::class.java), bindHandler)
