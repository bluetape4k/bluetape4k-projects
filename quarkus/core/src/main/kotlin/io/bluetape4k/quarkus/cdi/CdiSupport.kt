package io.bluetape4k.quarkus.cdi

import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.spi.CDI

inline fun <reified T: Any> CDI<Any>.selectAs(vararg qualifiers: Annotation): Instance<T> =
    select(T::class.java, *qualifiers)
