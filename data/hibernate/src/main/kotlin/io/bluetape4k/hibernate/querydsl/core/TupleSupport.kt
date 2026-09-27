package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.Tuple

inline fun <reified T> Tuple.getAs(index: Int): T =
    get(index, T::class.java) as T
