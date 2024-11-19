package io.bluetape4k.spring.cassandra

import org.springframework.data.cassandra.core.ReactiveSelectOperation.SelectWithProjection
import org.springframework.data.cassandra.core.ReactiveSelectOperation.SelectWithQuery

inline fun <reified R: Any> SelectWithProjection<*>.cast(): SelectWithQuery<R> =
    `as`(R::class.java)
