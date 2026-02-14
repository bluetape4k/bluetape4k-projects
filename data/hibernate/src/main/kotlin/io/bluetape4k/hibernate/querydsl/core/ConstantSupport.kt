package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Constant
import com.querydsl.core.types.ConstantImpl

/**
 * QueryDSL의 [Constant]를 생성합니다.
 */
fun constantOf(value: Boolean): Constant<Boolean> = ConstantImpl.create(value)

/**
 * QueryDSL의 [Constant]를 생성합니다.
 */
fun constantOf(value: Char): Constant<Char> = ConstantImpl.create(value)

/**
 * QueryDSL의 [Constant]를 생성합니다.
 */
fun constantOf(value: Byte): Constant<Byte> = ConstantImpl.create(value)

/**
 * QueryDSL의 [Constant]를 생성합니다.
 */
fun constantOf(value: Int): Constant<Int> = ConstantImpl.create(value)

/**
 * QueryDSL의 [Constant]를 생성합니다.
 */
fun constantOf(value: Long): Constant<Long> = ConstantImpl.create(value)

/**
 * QueryDSL의 [Constant]를 생성합니다.
 */
fun constantOf(value: Short): Constant<Short> = ConstantImpl.create(value)

/**
 * QueryDSL의 [Constant]를 생성합니다.
 */
inline fun <reified T> constantOf(constant: T): Constant<T> =
    ConstantImpl.create(T::class.java, constant)
