package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Constant
import com.querydsl.core.types.ConstantImpl

/**
 * QueryDSL의 Boolean [Constant]를 생성합니다.
 *
 * ```kotlin
 * val expr = constantOf(true)
 * // expr.constant == true
 * ```
 */
fun constantOf(value: Boolean): Constant<Boolean> = ConstantImpl.create(value)

/**
 * QueryDSL의 Char [Constant]를 생성합니다.
 *
 * ```kotlin
 * val expr = constantOf('A')
 * // expr.constant == 'A'
 * ```
 */
fun constantOf(value: Char): Constant<Char> = ConstantImpl.create(value)

/**
 * QueryDSL의 Byte [Constant]를 생성합니다.
 *
 * ```kotlin
 * val expr = constantOf(1.toByte())
 * // expr.constant == 1
 * ```
 */
fun constantOf(value: Byte): Constant<Byte> = ConstantImpl.create(value)

/**
 * QueryDSL의 Int [Constant]를 생성합니다.
 *
 * ```kotlin
 * val expr = constantOf(42)
 * // expr.constant == 42
 * ```
 */
fun constantOf(value: Int): Constant<Int> = ConstantImpl.create(value)

/**
 * QueryDSL의 Long [Constant]를 생성합니다.
 *
 * ```kotlin
 * val expr = constantOf(100L)
 * // expr.constant == 100L
 * ```
 */
fun constantOf(value: Long): Constant<Long> = ConstantImpl.create(value)

/**
 * QueryDSL의 Short [Constant]를 생성합니다.
 *
 * ```kotlin
 * val expr = constantOf(10.toShort())
 * // expr.constant == 10
 * ```
 */
fun constantOf(value: Short): Constant<Short> = ConstantImpl.create(value)

/**
 * QueryDSL의 임의 타입 [Constant]를 생성합니다.
 *
 * ```kotlin
 * val expr = constantOf("hello")
 * // expr.constant == "hello"
 * ```
 */
inline fun <reified T> constantOf(constant: T): Constant<T> =
    ConstantImpl.create(T::class.java, constant)
