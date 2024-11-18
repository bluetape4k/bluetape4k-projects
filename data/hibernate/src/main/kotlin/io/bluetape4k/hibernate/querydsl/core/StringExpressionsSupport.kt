package io.bluetape4k.hibernate.querydsl.core

import com.querydsl.core.types.Expression
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.core.types.dsl.StringExpression
import com.querydsl.core.types.dsl.StringExpressions

/**
 * [StringExpression]에 [other] [Expression]을 `concat` 합니다.
 */
operator fun StringExpression.plus(other: Expression<String>): StringExpression = this.append(other)

/**
 * [StringExpression]에 [str]을 `concat` 합니다.
 */
operator fun StringExpression.plus(str: String): StringExpression = this.append(str)

/**
 * [Expression]`<String>`의 left trim을 수행합니다.
 */
fun Expression<String>.ltrim(): StringExpression = StringExpressions.ltrim(this)

/**
 * [Expression]`<String>`의 right trim을 수행합니다.
 */
fun Expression<String>.rtrim(): StringExpression = StringExpressions.rtrim(this)

fun Expression<String>.lpad(length: Int): StringExpression =
    StringExpressions.lpad(this, length)

fun Expression<String>.lpad(length: Int, c: Char): StringExpression =
    StringExpressions.lpad(this, length, c)

fun Expression<String>.lpad(length: Expression<Int>): StringExpression =
    StringExpressions.lpad(this, length)

fun Expression<String>.lpad(length: NumberExpression<Int>, c: Char): StringExpression =
    StringExpressions.lpad(this, length, c)

fun Expression<String>.rpad(length: Int): StringExpression =
    StringExpressions.rpad(this, length)

fun Expression<String>.rpad(length: Int, c: Char): StringExpression =
    StringExpressions.rpad(this, length, c)


fun Expression<String>.rpad(length: Expression<Int>): StringExpression =
    StringExpressions.rpad(this, length)

fun Expression<String>.rpad(length: NumberExpression<Int>, c: Char): StringExpression =
    StringExpressions.rpad(this, length, c)
